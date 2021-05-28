package com.atguigu.gmall.index.feign;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
    @GetMapping("pms/category/subs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategory(@PathVariable("pid")Long pid);
    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoryList(@PathVariable("parentId")Long pid);
}
