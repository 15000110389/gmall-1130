package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

@Controller
public class IndexController {
    @Resource
    private IndexService indexService;
    @GetMapping
    public String Toindex(Model model) {
        List<CategoryEntity> categoryEntities=this.indexService.queryLv1();
        model.addAttribute("categories",categoryEntities);
        return "index";
    }
    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLv2(@PathVariable("pid")Long pid) {
        List<CategoryEntity> categoryEntities=this.indexService.queryLv2(pid);
        return  ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo test(){
        this.indexService.test();
        return ResponseVo.ok();
    }
    @GetMapping("index/test")
    @ResponseBody
    public ResponseVo test1(){
        this.indexService.test3();
        return ResponseVo.ok();
    }
}
