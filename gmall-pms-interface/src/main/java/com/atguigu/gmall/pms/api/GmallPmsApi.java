package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    @GetMapping("pms/spu/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);
    @PostMapping("pms/spu/page/json")
    @ApiOperation("分页查询")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuList(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/brand/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/spuattrvalue/search/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValuesBycidAndSpuId(
            @PathVariable("cid")Long cid, @RequestParam("spuId")Long spuId
    );

    @GetMapping("pms/skuattrvalue/search/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValuesBycidAndSkuId(
            @PathVariable("cid")Long cid, @RequestParam("skuId")Long skuId
    );
}
