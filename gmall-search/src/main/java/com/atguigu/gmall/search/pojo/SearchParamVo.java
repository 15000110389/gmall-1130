package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {

    // 关键字
    private String keyword;

    // 品牌过滤条件
    private List<Long> brandId;

    // 分类的过滤条件
    private List<Long> categoryId;

    // 规格参数过滤条件
    private List<String> props;

    // 排序条件：默认-得分排序，1-价格降序 2-价格升序 3-销量降序 4-新品降序
    private Integer sort;

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

    // 仅显示有货
    private Boolean store;

    private Integer pageNum=1;
    private final Integer pageSize = 20;
}
