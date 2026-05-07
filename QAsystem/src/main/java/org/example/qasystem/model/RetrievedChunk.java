package org.example.qasystem.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {

    private Long fileId;
    private String title;
    private String category;
    private Integer chunkIndex;
    /** 在原文中的起始行号（1-based） */
    private int startLine = 1;
    /** 在原文中的结束行号（1-based，含） */
    private int endLine = 1;
    private String content;
    private double score;
}
