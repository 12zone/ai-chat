package org.example.qasystem.service;

import java.util.List;

public interface EmbeddingService {
    List<Double> embed(String text);
}
