package com.zhan.exam.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhan.exam.entity.Category;
import com.zhan.exam.entity.Question;
import com.zhan.exam.mapper.CategoryMapper;
import com.zhan.exam.mapper.QuestionMapper;
import com.zhan.exam.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper,Category> implements CategoryService {

    @Autowired
    private QuestionMapper questionMapper;

    /**
     * 查询分类列表的同时查询分类的题目数量
     * @return
     */
    @Override
    public List<Category> findCategoryList() {
        //1.查询所有分类信息集合（单表操作）-> mybatis-plus 提供的list方法
        //List<Category> 每个分类对象没有count
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);
        List<Category> categoryList = list(queryWrapper);
        //------------------------------------------------
        //2.QuestionMapper定义查询方法，category_id进行分组，并且统计每个分类下的count
        //[ map{category_id:14,count:1} , {category_id:15,count:2} ]
        List<Map<String,Long>> mapList = questionMapper.selectCategoryQuestionCount();
        //3.题目查询的分类的题目数量赋值给分类集合
        //先 mapList -> map -> 14:1 , 15:2
        Map<Long, Long> collectMap = mapList.stream().collect(Collectors.toMap(m -> m.get("category_id"), m -> m.get("count")));
        //--------------------------------------------------
        //4.返回对应的查询集合即可
        for (Category category : categoryList) {
            Long id = category.getId();
            category.setCount(collectMap.getOrDefault(id,0L));
        }
        return categoryList;
    }

    /**
     * 查询分类的树状列表
     * @return
     */
    @Override
    public List<Category> findCategoryTreeList() {
        List<Category> categoryList = findCategoryList();
        // 分类信息进行分组
        //key - parent
        //value - list<子分类>
        Map<Long, List<Category>> longListMap = categoryList.stream().collect(Collectors.groupingBy(Category::getId));

        //筛选分类信息(获取一级分类)
        List<Category> parentCategoryList = categoryList.stream().filter(c -> c.getParentId() == 0).collect(Collectors.toList());

        //给一级分类循环，获取子分类，并且计算count（父分类count + 所有子分类count）
        for (Category parentCategory : parentCategoryList) {
            List<Category> sonCategoryList = longListMap.getOrDefault(parentCategory.getId(), new ArrayList<>());
            parentCategory.setChildren(sonCategoryList);
            //count
            Long sonCount = sonCategoryList.stream().collect(Collectors.summingLong(Category::getCount));
            parentCategory.setCount(parentCategory.getCount() + sonCount);
        }
        return parentCategoryList;
    }

    /**
     * 添加题目分类
     * @param category
     */
    @Override
    public void saveCategory(Category category) {
        //1.当前父分类下不能存在同名的子分类
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getParentId,category.getParentId());
        queryWrapper.eq(Category::getName,category.getName());
        if(count(queryWrapper)>0){
            throw new RuntimeException("%s父分类下，已经存在名为：%s的子分类信息！本次添加失败！"
                    .formatted(category.getParentId(),category.getName()));
        }
        //2.保存分类即可
        save(category);
    }

    /**
     * 保存分类信息
     */
    @Override
    public void updateCategory(Category category) {
        //1.校验 同一个父类，不能与其他子分类的name相同，可以和自己原来name相同
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getParentId,category.getParentId());//限定同一个父分类下查询
        queryWrapper.eq(Category::getName,category.getName());//找同名的分类
        queryWrapper.ne(Category::getId,category.getId());//检查其他的子分类,排除当前要修改的这条数据，只查别人
        //parent_id = 3 AND name = '微服务架构' AND id != 1
        long count = count(queryWrapper);
        if(count>0){
            throw new RuntimeException("%s父分类下，已经存在名为：%s的子分类信息！本次添加失败！"
                    .formatted(category.getParentId(),category.getName()));
        }
        //2.更新分类信息
        updateById(category);
    }

    /**
     * 删除题目分类
     * @param id
     */
    @Override
    public void removeCategory(Long id) {
        //1.先判断是不是一级分类
        Category category = getById(id);
        if(category==null){
            log.info("在删除之前已经被删除！！");
            return ;
        }
        if(category.getParentId()== 0){
            //一级分类
            throw new RuntimeException("id = %s的分类为一级分类，不允许删除！".formatted(id));
        }
        //2.判定有没有关联的题目
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getCategoryId,id);
        Long count = questionMapper.selectCount(queryWrapper);
        if(count>0){
            throw new RuntimeException("id = %s的分类删除失败！因为下面有关联的题目，关联的题目数量为：%s".formatted(id,count));
        }
        //3.才可以进行删除分类
        removeById(id);
    }


}