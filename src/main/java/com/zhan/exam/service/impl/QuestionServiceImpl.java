package com.zhan.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhan.exam.common.CacheConstants;
import com.zhan.exam.entity.PaperQuestion;
import com.zhan.exam.entity.Question;
import com.zhan.exam.entity.QuestionAnswer;
import com.zhan.exam.entity.QuestionChoice;
import com.zhan.exam.mapper.PaperQuestionMapper;
import com.zhan.exam.mapper.QuestionAnswerMapper;
import com.zhan.exam.mapper.QuestionChoiceMapper;
import com.zhan.exam.mapper.QuestionMapper;
import com.zhan.exam.service.QuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhan.exam.utils.RedisUtils;
import com.zhan.exam.vo.QuestionQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private RedisUtils redisUtils;

    /**
     * 分页查询题目信息，分布查询
     * @param questionPage
     * @param questionQueryVo
     */
    @Override
    public void queryQuestionListByPage(Page<Question> questionPage, QuestionQueryVo questionQueryVo) {
        questionMapper.selectQuestionPage(questionPage,questionQueryVo);
    }

    /**
     * 查询题目的详情
     *      方案1： 嵌套结果 连表查询 + result [可以使用，没有分页]
     *      方案2： 嵌套查询 分布查询 [可以使用 没必要 1+n]
     *      方案3： 查询+java代码赋值即可
     * @param id
     * @return
     */
    @Override
    public Question queryQuestionById(Long id) {
        //1.查询题目详情对象
        Question question = getById(id);
        if(question!=null){
            throw new RuntimeException("查询id为%s的题目已经不存在！".formatted(id));
        }
        //2.查询题目对应的答案
        QuestionAnswer questionAnswer = questionAnswerMapper
                .selectOne(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId,id));
        question.setAnswer(questionAnswer);
        //3.查询题目对应的选项（选择题才有选项）
        if("CHOICE".equals(question.getType())){
            List<QuestionChoice> questionChoices = questionChoiceMapper
                    .selectList(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,id));
            question.setChoices(questionChoices);
        }
        //4.题目详情赋值

        //5.预留：进行redis的数据缓存zset
        new Thread(() -> {
            incrementQuestionScore(question.getId());
        }).start();

        return question;
    }

    /**
     * 保存题目信息
     *
     * @param question
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveQuestion(Question question) {
        //1.先判断不能重复 同一type类型下（选择题 简单 判断题）title不能重复
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getType,question.getType());
        queryWrapper.eq(Question::getTitle,question.getTitle());
        long count = count(queryWrapper);
        if(count>0){
            throw new RuntimeException("在%s类型下，已经存在名为%s的题目信息，保存失败"
                    .formatted(question.getType(),question.getTitle()));
        }
        //2.保存题目信息（先保存题目，保存题目才有题目id，才能进行后续的答案和选项保存）
        save(question);
        //3.判断是不是选择题，是根据选项的正确给答案赋值 同时将选项插入到选项表
        QuestionAnswer questionAnswer = new QuestionAnswer();
        questionAnswer.setQuestionId(question.getId());
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> questionChoices = question.getChoices();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < questionChoices.size(); i++) {
                QuestionChoice choice = questionChoices.get(i);
                choice.setSort(i);
                choice.setQuestionId(question.getId());
                //保存选项 [循环 + 数据库保存]
                questionChoiceMapper.insert(choice);
                //A,D
                if (choice.getIsCorrect()) {
                    if (sb.length() > 0){
                        sb.append(",");
                    }
                    sb.append((char)('A' + i));
                }
                //通过选项给答案赋值
                questionAnswer.setAnswer(sb.toString());
            }
        }
        //4.完成答案数据的插入
        questionAnswerMapper.insert(questionAnswer);
    }

    /**
     * 更新题目及其完整信息（包含选项和答案）
     * <p>
     * 业务复杂性：
     * - 需要处理选项的增删改：删除旧选项，添加新选项
     * - 答案更新：覆盖原有答案或新增答案
     * - 数据完整性：确保更新过程中数据一致
     * <p>
     * 实现策略：
     * 1. 更新题目主表信息
     * 2. 删除原有选项，重新插入新选项（简化逻辑）
     * 3. 更新或插入答案信息
     *
     * @param question 包含更新信息的题目对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customUpdateQuestion(Question question) {
        //题目的校验（不同id不运行title重复）
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getTitle,question.getTitle());
        queryWrapper.eq(Question::getId,question.getId());
        boolean exists = baseMapper.exists(queryWrapper);
        if(exists){
            throw new RuntimeException("修改：%s题目的新标题：%s和其他的题目重复了！修改失败！".formatted(question.getId(),question.getTitle()));
        }
        //2. 修改题目
        boolean updated = updateById(question);
        if (!updated){
            throw new RuntimeException("修改：%s题目失败！！".formatted(question.getId()));
        }
        //3. 获取答案对象
        QuestionAnswer answer = question.getAnswer();
        //4. 判断是选择题
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> questionChoices = question.getChoices();
            //删除题目对应的所有选项（原） [根据题目id删除]
            LambdaQueryWrapper<QuestionChoice> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(QuestionChoice::getQuestionId,question.getId());
            questionChoiceMapper.delete(queryWrapper1);
            //循环新增选项（选项上id == null）
            // 拼接正确的档案 a,b
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < questionChoices.size(); i++) {
                QuestionChoice choice = questionChoices.get(i);
                choice.setId(null);
                //确保，正确顺序！ 否则默认是0 随机了
                choice.setSort(i);
                choice.setCreateTime(null);
                choice.setUpdateTime(null);
                //新增选项需要！！
                choice.setQuestionId(question.getId());
                questionChoiceMapper.insert(choice);
                if (choice.getIsCorrect()) {
                    if (sb.length() > 0){
                        sb.append(",");
                    }
                    sb.append((char)('A' + i));
                }
            }
            //答案对象赋值选择题答案
            answer.setAnswer(sb.toString());
        }
        //5.进行答案的修改
        questionAnswerMapper.updateById(answer);
        //6.添加事务
    }

    /**
     * 删除题目
     * 实现策略：
     * 1. 判断试卷是有有引用题目，有，删除失败！提示！
     * 2. 先删除子数据（选项和答案）
     * 3. 删除主数据题目表
     *
     * @param id
     */
    @Override
    public void customRemoveQuestionById(Long id) {
        //1.判断试卷题目表，存在删除失败
        LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperQuestion::getQuestionId,id);
        Long count = paperQuestionMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new RuntimeException("该题目：%s 被试卷表中引用%s次，删除失败！".formatted(id,count));
        }
        //2. 删除主表 题目表
        boolean removed = removeById(id);
        if (!removed){
            throw new RuntimeException("该题目：%s 信息删除失败！！");
        }
        //3. 删除子表 答案和选项表
        questionAnswerMapper.delete(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId,id));
        questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId,id));
    }

    /**
     * 查询指定数量的热门题目
     *
     * @param size 默认：6
     * @return
     */
    @Override
    public List<Question> customFindPopularQuestions(Integer size) {
//        1.定义热门题目集合（最终返回用）
//        List<Question> popularQuestions = new ArrayList<>();
        List<Question> popularQuestions = new ArrayList<>();
//        2.从 Redis ZSet 取热门题目ID（按分数从高到低取前 size 条）
//        redisUtils.zReverseRangeWithScores("question:popular", 0, size-1)
//            → 拿到 [(id=63, score=15), (id=12, score=8), ...]
//            → 按分数排序，提取ID列表 idsList
//            → 用 idsList 去数据库查题目，加入 popularQuestions
//        注意：Redis 里有的ID可能在数据库已被删除，要跳过
        Set<ZSetOperations.TypedTuple<Object>> tupleSet = redisUtils.zReverseRangeWithScores(CacheConstants.POPULAR_QUESTIONS_KEY,0,size-1);
        //定义接收id的集合
        List<Long> idsSet = new ArrayList<>();
        if (tupleSet != null && tupleSet.size() > 0) {
            //格局排行榜的积分，倒叙进行id查询
            List<Long> idsList = tupleSet.stream()
                    .sorted(((o1, o2) -> Integer.compare(o2.getScore().intValue(),o1.getScore().intValue())))
                    .map(o ->Long.valueOf(o.getValue().toString())).collect(Collectors.toList());
            //复制，用于后面补充！！
            idsSet.addAll(idsList);
            log.debug("从redis获取热门题目的id集合，且保证顺序：{}",idsList);

            for (Long id : idsList) {
                Question question = getById(id);
                if (question != null){
                    //防止redis有缓存，但是数据库中没有！ 后续优化，删除题目，应该删除热题榜单中对应的value
                    popularQuestions.add(question);
                }
            }
            log.debug("去redis查询的热门题目，题目数：{},题目内容为：{}",popularQuestions.size(),popularQuestions);
        }
//        3.判断数量是否够 size
//        diff = size - popularQuestions.size()
//        如果 diff > 0（不够）：
//            → 查最新题目补足：SELECT * FROM questions WHERE id NOT IN (热门ID列表)
//        ORDER BY create_time DESC LIMIT diff
//            → 加入 popularQuestions
        int diff = size - popularQuestions.size();
        if(diff > 0){
            // 不满足，题目表中 非热门题目 时间倒序 limit 差数量
            LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.notIn(Question::getId,idsSet);
            lambdaQueryWrapper.orderByDesc(Question::getCreateTime);
            //limit diff;
            lambdaQueryWrapper.last("limit " + diff);
            List<Question> questionDiffList = list(lambdaQueryWrapper);
            log.debug("去question表中补充热门题目，题目数：{},题目内容为：{}",questionDiffList.size(),questionDiffList);
            if (questionDiffList != null && questionDiffList.size() > 0) {
                // 5. 补充也添加到总集合中
                popularQuestions.addAll(questionDiffList);
            }
        }
//        4.给所有题目填充选项和答案
//        复用已有的 fillQuestionChoiceAndAnswer(popularQuestions) 方法
//            （批量查选项和答案 → 转Map → 循环赋值）
        fillQuestionChoiceAndAnswer(popularQuestions);
//        5.返回 popularQuestions
        return popularQuestions;
    }


    /**
     * 批量填充题目的选项和答案（方案3：Java代码 + Stream 流）
     *
     * 核心思路：
     *   不依赖 MyBatis XML 的嵌套查询，用3次简单SQL + Java内存拼接来解决 N+1 问题
     *   不管多少道题，永远是 1(查题目) + 1(查选项) + 1(查答案) = 3次SQL
     *
     * @param questionList 题目列表（此时只有题目基本信息，choices 和 answer 都是 null）
     */
    private void fillQuestionChoiceAndAnswer(List<Question> questionList) {
        // ===== 第0步：非空判断 =====
        if (questionList == null || questionList.size() == 0) {
            log.debug("题目集合为空，跳过选项和答案填充");
            return;
        }

        // ===== 第1步：提取所有题目ID =====
        List<Long> ids = questionList.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        // ids = [63, 59, 54, 39, ...]

        // ===== 第2步：批量查所有选项（1次SQL） =====
        // SQL: SELECT * FROM question_choices WHERE question_id IN (63, 59, 54, ...) AND is_deleted = 0
        List<QuestionChoice> allChoices = questionChoiceMapper.selectList(
                new LambdaQueryWrapper<QuestionChoice>().in(QuestionChoice::getQuestionId, ids));

        // ===== 第3步：批量查所有答案（1次SQL） =====
        // SQL: SELECT * FROM question_answers WHERE question_id IN (63, 59, 54, ...) AND is_deleted = 0
        List<QuestionAnswer> allAnswers = questionAnswerMapper.selectList(
                new LambdaQueryWrapper<QuestionAnswer>().in(QuestionAnswer::getQuestionId, ids));

        // ===== 第4步：转成 Map，方便按题目ID快速查找 =====
        // 选项：List → Map<题目ID, 该题目的选项List>
        //   groupingBy 按 questionId 分组
        //   效果：{63=[ChoiceA, ChoiceB, ChoiceC, ChoiceD], 59=[], 54=[ChoiceA, ChoiceB], ...}
        Map<Long, List<QuestionChoice>> choiceMap = allChoices.stream()
                .collect(Collectors.groupingBy(QuestionChoice::getQuestionId));

        // 答案：List → Map<题目ID, 该题目的答案对象>
        //   toMap 一对一映射（每道题只有一个答案）
        //   效果：{63=Answer("A,B,D"), 59=Answer("TRUE"), 54=Answer("TRUE"), ...}
        Map<Long, QuestionAnswer> answerMap = allAnswers.stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, a -> a));

        // ===== 第5步：循环赋值（纯内存操作，不查数据库） =====
        questionList.forEach(question -> {
            // 每道题一定有一个答案，从 answerMap 里取
            question.setAnswer(answerMap.get(question.getId()));

            // 只有选择题才有选项
            if ("CHOICE".equals(question.getType())) {
                List<QuestionChoice> choices = choiceMap.get(question.getId());
                if (choices != null) {
                    // 按 sort 字段排序，保证选项以 A B C D 的顺序展示
                    choices.sort(Comparator.comparingInt(QuestionChoice::getSort));
                    question.setChoices(choices);
                }
            }
        });
    }

    private void incrementQuestionScore(Long questionId){
        Double score = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY, questionId, 1);
        log.debug("完成id：{}题目的热榜分数累计，累计后分数为：{}",questionId,score);
    }
}