package com.marketlens.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketlens.backend.entity.MarketReviewEntity;
import com.marketlens.backend.exception.NotFoundException;
import com.marketlens.backend.mapper.MarketReviewMapper;
import com.marketlens.backend.model.MarketReview;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketReviewService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final MarketReviewMapper marketReviewMapper;
    private final ObjectMapper objectMapper;

    public MarketReviewService(MarketReviewMapper marketReviewMapper, ObjectMapper objectMapper) {
        this.marketReviewMapper = marketReviewMapper;
        this.objectMapper = objectMapper;
    }

    public MarketReview getLatest() {
        MarketReviewEntity entity = marketReviewMapper.selectOne(new LambdaQueryWrapper<MarketReviewEntity>()
                .orderByDesc(MarketReviewEntity::getReviewDate)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new NotFoundException("Daily market review not found");
        }
        return toModel(entity);
    }

    public MarketReview saveOrUpdate(MarketReview review) {
        MarketReviewEntity existing = marketReviewMapper.selectOne(new LambdaQueryWrapper<MarketReviewEntity>()
                .eq(MarketReviewEntity::getReviewDate, review.reviewDate())
                .last("LIMIT 1"));

        MarketReviewEntity entity = existing == null ? new MarketReviewEntity() : existing;
        entity.setReviewDate(review.reviewDate());
        entity.setTitle(review.title());
        entity.setHighlightsJson(toJson(review.highlights()));
        entity.setLeadingStocksJson(toJson(review.leadingStocks()));
        entity.setSource(review.source());

        if (existing == null) {
            marketReviewMapper.insert(entity);
        } else {
            marketReviewMapper.updateById(entity);
        }
        return toModel(marketReviewMapper.selectById(entity.getId()));
    }

    private MarketReview toModel(MarketReviewEntity entity) {
        return new MarketReview(
                entity.getReviewDate(),
                entity.getTitle(),
                fromJson(entity.getHighlightsJson()),
                fromJson(entity.getLeadingStocksJson()),
                entity.getSource()
        );
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize market review", exception);
        }
    }

    private List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value == null ? "[]" : value, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize market review", exception);
        }
    }
}
