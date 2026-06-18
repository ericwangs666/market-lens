package com.marketlens.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketlens.backend.entity.Stock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
}
