package com.zhan.exam.mapper;

import com.zhan.exam.entity.Exam;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
    // 可以在这里添加自定义的查询方法
} 