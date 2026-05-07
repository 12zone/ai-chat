package org.example.qasystem.model;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内存向量索引中的一条块。向量使用 {@code float[]} 以降低堆占用（相对 {@code List<Double>}）。
 */
@Data
@NoArgsConstructor
public class IndexedChunk {

    private Long fileId;
    private String title;
    private String category;
    private Integer chunkIndex;
    /** 在原文中的起始行号（1-based），按分块窗口估算 */
    private int startLine = 1;
    /** 在原文中的结束行号（1-based，含） */
    private int endLine = 1;
    private String content;
    private float[] vector;
}
