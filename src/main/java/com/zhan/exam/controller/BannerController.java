package com.zhan.exam.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhan.exam.common.Result;
import com.zhan.exam.entity.Banner;
import com.zhan.exam.service.BannerService;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * 轮播图控制器 - 处理轮播图管理相关的HTTP请求
 * 包括图片上传、轮播图的CRUD操作、状态切换等功能
 */
@Slf4j
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/banners")  // 轮播图API路径前缀
@CrossOrigin  // 允许跨域访问
@Tag(name = "轮播图管理", description = "轮播图相关操作，包括图片上传、轮播图增删改查、状态管理等功能")  // Swagger API分组
public class BannerController {

    @Autowired
    private BannerService bannerService;

    
    /**
     * 上传轮播图图片
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping("/upload-image")  // 处理POST请求
    @Operation(summary = "上传轮播图图片", description = "将图片文件上传到MinIO服务器，返回可访问的图片URL")  // API描述
    public Result<String> uploadBannerImage(
            @Parameter(description = "要上传的图片文件，支持jpg、png、gif等格式，大小限制5MB")
            @RequestParam("file") MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String imageUrl = bannerService.uploadBannerImage(file);
        log.info("轮播图上传接口调用成功！图片回显地址为：{}", imageUrl);
        return Result.success(imageUrl, "图片上传成功");
    }
    
    /**
     * 获取启用的轮播图（前台首页使用）
     * @return 轮播图列表
     */
    @GetMapping("/active")  // 处理GET请求
    @Operation(summary = "获取启用的轮播图", description = "获取状态为启用的轮播图列表，供前台首页展示使用")  // API描述
    public Result<List<Banner>> getActiveBanners() {
        //1.拼接查询条件（激活状态+保证正序排序）
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Banner::getIsActive, true);
        queryWrapper.orderByAsc(Banner::getSortOrder);
        //2.查询符合条件的数据
        List<Banner> bannerList = bannerService.list(queryWrapper);
        //3.装到返回结果的result
        Result<List<Banner>> result = Result.success(bannerList);
        //        --log日志输出
        log.info("查询前台激活轮播图接口调用成功！查询数量为：{}，具体数据为：{}",bannerList.size(),bannerList);
        //4.返回result数据
        return result;
    }
    
    /**
     * 获取所有轮播图（管理后台使用）
     * @return 轮播图列表
     */
    @GetMapping("/list")  // 处理GET请求
    @Operation(summary = "获取所有轮播图", description = "获取所有轮播图列表，包括启用和禁用的，供管理后台使用")  // API描述
    public Result<List<Banner>> getAllBanners() {
        //1.查询所有的轮播图数据集合
        //todo:解决返回轮播图的字段没有排序的问题
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Banner::getSortOrder);
        List<Banner> bannerList = bannerService.list(queryWrapper);
        //2.将集合装入result类中
        Result<List<Banner>> result = Result.success(bannerList);
        //        -- 日志输出 info -> 输出本次查询结果[logback]
        log.info("后台管理部分查询所有轮播图信息成功！查询轮播图数量为:{}，具体数据为{}",bannerList.size(),bannerList);
        //3.返回结果
        return result;
    }
    
    /**
     * 根据ID获取轮播图
     * @param id 轮播图ID
     * @return 轮播图详情
     */
    @GetMapping("/{id}")  // 处理GET请求
    @Operation(summary = "根据ID获取轮播图", description = "根据轮播图ID获取单个轮播图的详细信息")  // API描述  
    public Result<Banner> getBannerById(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        //1.根据id查询轮播图对象
        Banner byId = bannerService.getById(id);
        //        --日志输出
        log.info("查询id={}的轮播图结果调用成功，查询结果为{}",id,byId);
        //2.对象装入result返回
        return Result.success(byId);
    }
    
    /**
     * 添加轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PostMapping("/add")  // 处理POST请求
    @Operation(summary = "添加轮播图", description = "创建新的轮播图，需要提供图片URL、标题、跳转链接等信息")  // API描述
    public Result<String> addBanner(@RequestBody Banner banner) {
        bannerService.save(banner);
        log.info("保存轮播图数据成功，保存的id为：{}",banner.getId());
        return Result.success(null);
    }
    
    /**
     * 更新轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PutMapping("/update")  // 处理PUT请求
    @Operation(summary = "更新轮播图", description = "更新轮播图的信息，包括图片、标题、跳转链接、排序等")  // API描述
    public Result<String> updateBanner(@RequestBody Banner banner) {
        bannerService.updateById(banner);
        log.info("更新{}轮播图数据成功",banner.getId());
        return Result.success(null);
    }
    
    /**
     * 删除轮播图
     * @param id 轮播图ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")  // 处理DELETE请求
    @Operation(summary = "删除轮播图", description = "根据ID删除指定的轮播图")  // API描述
    public Result<String> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        //1.根据id删除对应的轮播图
        bannerService.removeById(id);
        //        --日志输出
        log.info("删除id={}的轮播图成功！",id);
        //2.返回对应删除结果
        return Result.success("删除成功");
    }
    
    /**
     * 启用/禁用轮播图
     * @param id 轮播图ID
     * @param isActive 是否启用
     * @return 操作结果
     */
    @PutMapping("/toggle/{id}")  // 处理PUT请求
    @Operation(summary = "切换轮播图状态", description = "启用或禁用指定的轮播图，禁用后不会在前台显示")  // API描述
    public Result<String> toggleBannerStatus(
            @Parameter(description = "轮播图ID") @PathVariable Long id, 
            @Parameter(description = "是否启用，true为启用，false为禁用") @RequestParam Boolean isActive) {
        //1.创建UpdateWrapper装修改的条件和修改后的值
        LambdaUpdateWrapper<Banner> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Banner::getId,id);
        updateWrapper.set(Banner::getIsActive, isActive);
        //2.调用业务层修改方法 update(wrapper)
        bannerService.update(updateWrapper);
        //        --日志输出
        log.info("id={}轮播图的状态修改成功！修改后的状态为：{}",id,isActive);
        //3.返回result.success(null)
        return Result.success(null);
    }
} 