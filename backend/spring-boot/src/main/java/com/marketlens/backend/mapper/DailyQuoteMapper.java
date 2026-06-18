package com.marketlens.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketlens.backend.entity.DailyQuote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyQuoteMapper extends BaseMapper<DailyQuote> {
}
