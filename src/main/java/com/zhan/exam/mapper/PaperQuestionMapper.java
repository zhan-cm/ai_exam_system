package com.zhan.exam.mapper;

import com.zhan.exam.entity.PaperQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @description 针对表【paper_question(试卷-题目关联表)】的数据库操作Mapper
* @createDate 2025-06-20 22:37:43
* @Entity com.exam.entity.PaperQuestion
*/
@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {

} 