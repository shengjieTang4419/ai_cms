package com.cloud.cloud.ai.chat.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final PgVectorStore vectorStore;

    /**
     * 解析多种格式文档
     */
    public void parseAndStoreDocument(MultipartFile file, Map<String, Object> metadata) {
        try {
            String content = extractContent(file);
            if (StringUtils.hasText(content)) {
                storeDocumentChunks(content, metadata);
            }
        } catch (Exception e) {
            log.error("文档解析失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档解析失败", e);
        }
    }

    /**
     * 上传并处理文档的完整流程
     */
    public DocumentUploadResult uploadAndProcessDocument(MultipartFile file, String title, String description, String tags) {
        try {
            // 构建metadata
            Map<String, Object> metadata = buildDocumentMetadata(file, title, description, tags);

            // 解析并存储文档
            parseAndStoreDocument(file, metadata);

            return DocumentUploadResult.builder()
                    .success(true)
                    .message("文档上传成功")
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .documentType(getDocumentType(file))
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("文档上传失败: {}", file.getOriginalFilename(), e);
            return DocumentUploadResult.builder()
                    .success(false)
                    .message("文档上传失败: " + e.getMessage())
                    .fileName(file.getOriginalFilename())
                    .build();
        }
    }

    /**
     * 构建文档的metadata信息
     */
    private Map<String, Object> buildDocumentMetadata(MultipartFile file, String title, String description, String tags) {
        Map<String, Object> metadata = new HashMap<>();

        // 基本文件信息
        metadata.put("fileName", file.getOriginalFilename());
        metadata.put("fileSize", file.getSize());
        metadata.put("contentType", file.getContentType());
        metadata.put("uploadTime", java.time.LocalDateTime.now());

        // 用户提供的元数据
        if (title != null && !title.trim().isEmpty()) {
            metadata.put("title", title);
        }
        if (description != null && !description.trim().isEmpty()) {
            metadata.put("description", description);
        }
        if (tags != null && !tags.trim().isEmpty()) {
            metadata.put("tags", tags);
        }

        // 从文件名推断文档类型
        String documentType = getDocumentType(file);
        metadata.put("documentType", documentType);

        // 添加用户ID（暂时写死，后续通过security获取）
        metadata.put("userId", 1L);

        return metadata;
    }

    /**
     * 获取文档类型
     */
    private String getDocumentType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * @param file
     * @return
     * @throws Exception
     * @see https://tika.apache.org/3.1.0/formats.html
     */
    private String extractContent(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        return switch (extension) {
            case "md", "pdf", "doc", "docx", "ppt", "pptx", "html", "htm", "txt", "csv" -> parse(file);
            default -> throw new UnsupportedOperationException("不支持的文件格式: " + extension);
        };
    }


    private String parse(MultipartFile file) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        // 增加内存限制，提高解析效果
        BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024); // 10MB
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        // 对于PPT文件，设置特殊参数
        String fileName = file.getOriginalFilename();
        if (fileName != null && (fileName.toLowerCase().endsWith(".ppt") || fileName.toLowerCase().endsWith(".pptx"))) {
            // 设置PPT解析参数
            metadata.set("org.apache.tika.parser.microsoft.office.PowerPointParser.includeSlideNotes", "true");
            metadata.set("org.apache.tika.parser.microsoft.office.PowerPointParser.includeSlideMasterContent", "true");
        }

        parser.parse(file.getInputStream(), handler, metadata, context);
        String content = handler.toString();

        // 记录解析结果
        log.info("文档解析完成: {}, 内容长度: {}", file.getOriginalFilename(), content.length());
        log.debug("解析内容预览: {}", content.length() > 200 ? content.substring(0, 200) + "..." : content);

        return content;
    }

    private void storeDocumentChunks(String content, Map<String, Object> metadata) {
        // 检查内容是否为空或过短
        if (content == null || content.trim().length() < 10) {
            log.warn("文档内容过短，跳过存储: {}", content);
            return;
        }

        // 文档分块策略 - 根据内容长度调整分块大小
        int maxChunkSize = content.length() < 5000 ? 500 : 1000; // 短文档用更小的块
        List<String> chunks = chunkTextBySemantic(content, maxChunkSize, 200);

        if (chunks.isEmpty()) {
            log.warn("文档分块后为空，跳过存储");
            return;
        }

        List<Document> documents = chunks.stream()
                .map(chunk -> {
                    // 为每个chunk添加块序号，便于后续追踪
                    Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                    chunkMetadata.put("chunkIndex", chunks.indexOf(chunk));
                    chunkMetadata.put("chunkSize", chunk.length());
                    return new Document(chunk, chunkMetadata);
                })
                .toList();

        vectorStore.add(documents);
        log.info("成功存储 {} 个文档块，总内容长度: {}", documents.size(), content.length());
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        // 简单的文本分块实现
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = Math.max(end - overlap, end);
            if (start >= text.length()) break;
        }

        return chunks;
    }

    /**
     * 语义化文本分块 - 按句子或段落边界分块，保持语义完整性
     */
    private List<String> chunkTextBySemantic(String text, int maxChunkSize, int overlap) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 清理文本，移除多余的空白字符
        text = text.replaceAll("\\s+", " ").trim();

        // 按段落分隔（双换行符）
        String[] paragraphs = text.split("\n\\s*\n");

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 如果当前段落本身就超过最大大小，先单独处理
            if (paragraph.length() > maxChunkSize) {
                // 如果当前块有内容，先保存
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentSize = 0;
                }

                // 将大段落按句子分割
                List<String> sentenceChunks = splitLargeParagraph(paragraph, maxChunkSize, overlap);
                chunks.addAll(sentenceChunks);
                continue;
            }

            // 如果加入当前段落后会超过限制
            if (currentSize + paragraph.length() > maxChunkSize && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString());

                // 开始新块，包含部分重叠内容
                String overlapText = extractOverlapFromChunk(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText).append(paragraph);
                currentSize = currentChunk.length();
            } else {
                // 直接添加段落
                if (!currentChunk.isEmpty()) {
                    currentChunk.append("\n\n");
                    currentSize += 2;
                }
                currentChunk.append(paragraph);
                currentSize += paragraph.length();
            }
        }

        // 添加最后一个块
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * 分割大段落为句子级别的块
     */
    private List<String> splitLargeParagraph(String paragraph, int maxChunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        // 中文句子结束符
        Pattern sentencePattern = Pattern.compile("[。！？；]");
        String[] sentences = sentencePattern.split(paragraph);

        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            if (i < sentences.length - 1) {
                // 重新添加分隔符
                sentence += paragraph.substring(
                        paragraph.indexOf(sentence) + sentence.length(),
                        Math.min(paragraph.indexOf(sentence) + sentence.length() + 1,
                                paragraph.length())
                );
            }

            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            // 如果句子本身就超过限制
            if (sentence.length() > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentSize = 0;
                }

                // 按字符分割大句子
                List<String> subChunks = chunkText(sentence, maxChunkSize, overlap);
                chunks.addAll(subChunks);
                continue;
            }

            // 检查是否需要新块
            if (currentSize + sentence.length() > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());

                // 开始新块，包含重叠
                String overlapText = extractOverlapFromChunk(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText).append(sentence);
                currentSize = currentChunk.length();
            } else {
                if (!currentChunk.isEmpty()) {
                    currentChunk.append(" ");
                    currentSize += 1;
                }
                currentChunk.append(sentence);
                currentSize += sentence.length();
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * 从文本块中提取重叠部分
     */
    private String extractOverlapFromChunk(String chunk, int overlapSize) {
        if (chunk.length() <= overlapSize) {
            return chunk;
        }

        // 尝试在句子边界处分割
        String[] sentences = chunk.split("[。！？；]");
        StringBuilder overlap = new StringBuilder();

        for (int i = sentences.length - 1; i >= 0; i--) {
            if (overlap.length() + sentences[i].length() <= overlapSize) {
                if (!overlap.isEmpty()) {
                    overlap.insert(0, sentences[i] + "。");
                } else {
                    overlap.append(sentences[i]).append("。");
                }
            } else {
                break;
            }
        }

        if (overlap.isEmpty()) {
            // 如果找不到合适的句子边界，退回到字符级分割
            return chunk.substring(Math.max(0, chunk.length() - overlapSize));
        }

        return overlap.toString();
    }

    /**
     * 文档上传结果
     */
    @Data
    @Builder
    public static class DocumentUploadResult {
        private boolean success;
        private String message;
        private String fileName;
        private Long fileSize;
        private String documentType;
        private Map<String, Object> metadata;
    }

}
