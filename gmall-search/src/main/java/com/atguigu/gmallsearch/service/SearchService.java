package com.atguigu.gmallsearch.service;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryBrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmallsearch.pojo.Goods;
import com.atguigu.gmallsearch.pojo.SearchParamVo;
import com.atguigu.gmallsearch.pojo.SearchResponseAttrVo;
import com.atguigu.gmallsearch.pojo.SearchResponseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {
    @Resource
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo searchParamVo) {

        try {
            SearchRequest request = new SearchRequest(new String[]{"goods"},builder(searchParamVo));
            SearchResponse response = this.restHighLevelClient.search(request, RequestOptions.DEFAULT);

            SearchResponseVo responseVo = this.parseResult(response);
             // 设置分页参数
            responseVo.setPageName(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());

            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits());
        SearchHit[] hitsHits = hits.getHits();
        if(hitsHits==null || hitsHits.length == 0){
            throw new RuntimeException("乱写");
        }

        List<Goods> goodList = Stream.of(hitsHits).map(hitsHit -> {
            String sourceAsString = hitsHit.getSourceAsString();
            try {
                Goods goods = MAPPER.readValue(sourceAsString, Goods.class);

                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                goods.setTitle(highlightField.getFragments()[0].toString());
                return goods;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        return null;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodList);

        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        ParsedLongTerms brandIdAgg =(ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            responseVo.setBrands(buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                Map<String, Aggregation> map = bucket.getAggregations().asMap();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)map.get("brandNameAgg");
                List<? extends Terms.Bucket> buckets1 = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets1)) {
                    brandEntity.setName(buckets1.get(0).getKeyAsString());
                }
                ParsedStringTerms logoAgg=(ParsedStringTerms)map.get("logoAgg");
                List<? extends Terms.Bucket> buckets2 = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets2)){
                    brandEntity.setLogo(buckets2.get(0).getKeyAsString());
                }

                return brandEntity;
            }).collect(Collectors.toList()));
        }


        // 获取分类的聚合结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        // 解析分类聚合结果集，或者桶集合
        List<? extends Terms.Bucket> buckets1 = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets1)){
            responseVo.setCategories (buckets1.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)bucket.getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> buckets2 = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets2)){
                    categoryEntity.setName(buckets2.get(0).getKeyAsString());
                }
                return  categoryEntity;
            }).collect(Collectors.toList()));
        }

        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets2 = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets2)){
            List<SearchResponseAttrVo> collect = buckets2.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();
                ParsedStringTerms attrNameAgg = (ParsedStringTerms)stringAggregationMap.get("attrNameAgg");
                List<? extends Terms.Bucket> buckets3 = attrNameAgg.getBuckets();
                if(!CollectionUtils.isEmpty(buckets3)){
                    searchResponseAttrVo.setAttrName(buckets3.get(0).getKeyAsString());
                }

                ParsedStringTerms attrValueAgg =(ParsedStringTerms) stringAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> buckets4 = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets4)){
                    searchResponseAttrVo.setAttrValues(buckets4.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }

                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(collect);
        }


        return responseVo;
    }

    private SearchSourceBuilder builder(SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        String keyword = paramVo.getKeyword();
        if(StringUtils.isBlank(keyword)){
            return sourceBuilder;
        }
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        sourceBuilder.query(boolQueryBuilder);
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));


        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }

        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }

        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceTo != null || priceTo != null) {
            RangeQueryBuilder queryBuilder = QueryBuilders.rangeQuery("price");

            if (priceFrom != null) {
                queryBuilder.gte(priceFrom);
            }
            if (priceTo != null) {
                queryBuilder.lte(priceTo);
            }
            boolQueryBuilder.filter(queryBuilder);
        }

        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("store",store));
        }
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop->{

                String[] split = StringUtils.split(prop, ":");
                if (split.length == 2&&split!=null) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",split[0]));
                    String[] split1 = StringUtils.split(split[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",split1));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                }
            });
        }

        Integer sort = paramVo.getSort();
        if (sort != null) {
            switch (sort){
                case 1 : sourceBuilder.sort("price", SortOrder.DESC);break;
                case 2 :sourceBuilder.sort("price", SortOrder.ASC);break;
                case 3 :sourceBuilder.sort("sales", SortOrder.DESC);break;
                case 4 :sourceBuilder.sort("createTime", SortOrder.DESC);break;
                default :sourceBuilder.sort("_score",SortOrder.DESC);break;
            }
        }

        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum*pageSize)-pageSize);
        sourceBuilder.size(pageSize);

        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red;'>").postTags("</font>"));

        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        sourceBuilder.fetchSource(new String[]{"skuId","title","subTitle","defaultImage","price"},null);
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
