package com.zhan.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhan.exam.common.Result;
import com.zhan.exam.entity.Category;
import com.zhan.exam.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器 - 处理题目分类管理相关的HTTP请求
 * 包括分类的增删改查、树形结构展示等功能
 */
@Slf4j
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/categories")  // 分类API路径前缀
@Tag(name = "分类管理", description = "题目分类相关操作，包括分类的增删改查、树形结构管理等功能")  // Swagger API分组
@CrossOrigin
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * todo:finished
     * 获取分类列表（包含题目数量）
     * @return 分类列表数据
     */
    @GetMapping  // 处理GET请求
    @Operation(summary = "获取分类列表", description = "获取所有题目分类列表，包含每个分类下的题目数量统计")  // API描述
    public Result<List<Category>> getCategories() {
        List<Category> allCategories = categoryService.findCategoryList();
        log.info("查询分类列表接口调用成功！，查询分类数量为：{}，具体数据内容为：{}",allCategories.size(),allCategories);
        return Result.success(allCategories);
    }

    /**
     * todo:finished
     * 获取分类树形结构
     * @return 分类树数据
     */
    @GetMapping("/tree")  // 处理GET请求
    @Operation(summary = "获取分类树形结构", description = "获取题目分类的树形层级结构，用于前端树形组件展示")  // API描述
    public Result<List<Category>> getCategoryTree() {
        List<Category> categoryTree = categoryService.findCategoryTreeList();
        log.info("查询树状的分类列表接口调用成功！查询的数量为：{}，具体数据为：{}",categoryTree.size(),categoryTree);
        return Result.success(categoryTree);
    }

    /**
     * todo:finished
     * 添加分类
     * @param category 分类对象
     * @return 操作结果
     */
    @PostMapping  // 处理POST请求
    @Operation(summary = "添加新分类", description = "创建新的题目分类，支持设置父分类实现层级结构")  // API描述
    public Result<Void> addCategory(@RequestBody Category category) {
        categoryService.saveCategory(category);
        log.info("保存：{}分类信息成功！，",category);
        return Result.success(null);
    }

    /**
     * todo:finished
     * 更新分类
     * @param category 分类对象
     * @return 操作结果
     */
    @PutMapping  // 处理PUT请求
    @Operation(summary = "更新分类信息", description = "修改分类的名称、描述、排序等信息")  // API描述
    public Result<Void> updateCategory(@RequestBody Category category) {
        categoryService.updateCategory(category);
        log.info("更新：{}分类信息成功！",category);
        return Result.success(null);
    }

    /**
     * 删除分类
     * @param id 分类ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")  // 处理DELETE请求
    @Operation(summary = "删除分类", description = "删除指定的题目分类，注意：删除前需确保分类下没有题目")  // API描述
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.removeCategory(id);
        log.info("删除id为{}的分类信息接口调用成功！",id);
        return Result.success(null);
    }
} 