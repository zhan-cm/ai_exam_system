package com.zhan.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhan.exam.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> findCategoryList();

    List<Category> findCategoryTreeList();

    void saveCategory(Category category);

    void updateCategory(Category category);

    void removeCategory(Long id);
}