package com.atguigu.gmallsearch.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmallsearch.pojo.SearchParamVo;
import com.atguigu.gmallsearch.pojo.SearchResponseVo;
import com.atguigu.gmallsearch.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
