package org.example.qasystem.service.impl;

import org.example.qasystem.model.RetrievedChunk;
import org.example.qasystem.service.ChatService;
import org.example.qasystem.service.VectorSearchService;
import org.example.qasystem.client.ModelChatClient;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service("chatService")
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final VectorSearchService vectorSearchService;

    public ChatServiceImpl(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public String chat(String message, String modelName) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        List<RetrievedChunk> chunks = vectorSearchService.search(message, null, 5);
        log.info("chat RAG model={} retrievedChunks={} (query embedded for vector similarity)", modelName, chunks.size());

        String context = chunks.isEmpty()
                ? "（知识库中暂无已索引片段：向量索引为空或未命中。）"
                : chunks.stream()
                .map(chunk -> String.format(
                        "[fileId=%d][title=%s][chunkIndex=%d][lines=%d-%d][score=%.3f]\n%s",
                        chunk.getFileId(),
                        chunk.getTitle() != null ? chunk.getTitle() : "",
                        chunk.getChunkIndex() != null ? chunk.getChunkIndex() : -1,
                        chunk.getStartLine(),
                        chunk.getEndLine(),
                        chunk.getScore(),
                        chunk.getContent()))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = "你是知识库问答助手。下面「知识片段」来自：用户问题经 **嵌入向量（embedding）** 与库中片段做相似度检索得到，请只依据这些片段作答。\n"
                + "规则：\n"
                + "1）片段中没有的信息请明确说知识库未涉及，勿编造。\n"
                + "2）可归纳、总结，但不要与片段明显矛盾。\n"
                + "3）系统会在你回答后自动附加「参考资料」列表（文件名、行号、fileId 等），正文内无需重复整表。\n\n"
                + context;

        ModelChatClient chatClient = LLMServiceFactory.getLLMService(modelName);
        if (chatClient == null) {
            throw new IllegalArgumentException("不支持的模型: " + modelName);
        }
        String reply = chatClient.chatWithSystem(systemPrompt, message);
        String appendix = buildReferenceAppendix(chunks);
        return reply + "\n\n---\n参考资料（RAG 检索命中的片段）\n" + appendix;
    }

    private static String buildReferenceAppendix(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "（未命中已索引片段：请上传文件并等待入库索引，或换关键词重试。）";
        }
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (RetrievedChunk c : chunks) {
            String title = c.getTitle() != null && !c.getTitle().isBlank() ? c.getTitle() : "(无标题)";
            int idx = c.getChunkIndex() != null ? c.getChunkIndex() : -1;
            sb.append(String.format(
                    "%d. 《%s》 fileId=%d，约第 %d–%d 行，chunkIndex=%d，相似度=%.3f%n",
                    n++, title, c.getFileId(), c.getStartLine(), c.getEndLine(), idx, c.getScore()));
        }
        return sb.toString().trim();
    }
}
