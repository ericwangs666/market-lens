package com.marketlens.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marketlens.backend.entity.StockNote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockNoteMapper extends BaseMapper<StockNote> {
}
