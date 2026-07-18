package com.zhan.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhan.exam.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhan.exam.vo.QuestionQueryVo;

import java.util.List;

/**
 * 题目业务服务接口 - 定义题目相关的业务逻辑
 * 
 * Spring Boot三层架构教学要点：
 * 1. Service层：业务逻辑层，位于Controller和Mapper之间
 * 2. 接口设计：定义业务方法规范，便于不同实现类的切换
 * 3. 继承IService：使用MyBatis Plus提供的通用服务接口，减少重复代码
 * 4. 事务管理：Service层是事务的边界，复杂业务操作应该加@Transactional
 * 5. 业务封装：将复杂的数据操作封装成有业务意义的方法
 * 
 * MyBatis Plus教学：
 * - IService<T>：提供基础的CRUD方法（save、update、remove、list等）
 * - 自定义方法：在接口中定义特定业务需求的方法
 * - 实现类：继承ServiceImpl<Mapper, Entity>并实现自定义业务方法
 * 
 * 设计原则：
 * - 单一职责：专门处理题目相关的业务逻辑
 * - 开闭原则：通过接口定义，便于扩展新的实现
 * - 依赖倒置：Controller依赖接口而不是具体实现
 * 
 * @author 智能学习平台开发团队
 * @version 1.0
 */
public interface QuestionService extends IService<Question> {

    /**
     * 分页查询题目信息：方案2 进行分布查询
     * @param questionPage
     * @param questionQueryVo
     */
    void queryQuestionListByPage(Page<Question> questionPage, QuestionQueryVo questionQueryVo);

    /**
     * 查询题目的详情
     * @param id
     * @return
     */
    Question queryQuestionById(Long id);

    /**
     * 保存题目信息
     * @param question
     */
    void saveQuestion(Question question);

    /**
     * 更新题目及其完整信息（包含选项和答案）
     *
     * 业务复杂性：
     * - 需要处理选项的增删改：删除旧选项，添加新选项
     * - 答案更新：覆盖原有答案或新增答案
     * - 数据完整性：确保更新过程中数据一致
     *
     * 实现策略：
     * 1. 更新题目主表信息
     * 2. 删除原有选项，重新插入新选项（简化逻辑）
     * 3. 更新或插入答案信息
     *
     * @param question 包含更新信息的题目对象
     */
    void customUpdateQuestion(Question question);

    /**
     * 删除题目
     * 实现策略：
     *  1. 判断试卷是有有引用题目，有，删除失败！提示！
     *  2. 先删除子数据（选项和答案）
     *  3. 删除主数据题目表
     * @param id
     */
    void customRemoveQuestionById(Long id);

    /**
     * 查询指定数量的热门题目
     * @param size 默认：6
     * @return
     */
    List<Question> customFindPopularQuestions(Integer size);
}