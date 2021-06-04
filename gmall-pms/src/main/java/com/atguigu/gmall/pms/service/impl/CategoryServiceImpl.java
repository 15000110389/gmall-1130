package com.atguigu.gmall.pms.service.impl;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;

import javax.annotation.Resource;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoryList(Long pid) {
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        if (pid != -1) {
            queryWrapper.eq("parent_id", pid);
        }

        return this.list(queryWrapper);
    }
    @Resource
    CategoryMapper categoryMapper;
    @Override
    public List<CategoryEntity> queryCategory(Long pid) {
        return categoryMapper.queryCategory(pid);
    }

    @Override
    public List<CategoryEntity> queryCatesByCid3(Long cid) {
        CategoryEntity categoryEntity3 = this.getById(cid);
        if (categoryEntity3 == null) {
            return null;
        }
        CategoryEntity categoryEntity2 = this.getById(categoryEntity3.getParentId());
        CategoryEntity categoryEntity = this.getById(categoryEntity2.getParentId());

        return Arrays.asList(categoryEntity,categoryEntity2,categoryEntity3);
    }

}