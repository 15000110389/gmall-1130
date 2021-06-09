package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;

@Controller
public class SearchController {
    @Resource
    private SearchService searchService;
    @GetMapping("search")
    public String search(SearchParamVo searchParamVo, Model model) {
        SearchResponseVo searchResponseVo=this.searchService.search(searchParamVo);
        model.addAttribute("response",searchResponseVo);
        model.addAttribute("searchParam",searchParamVo);
        return "search";
    }

}
