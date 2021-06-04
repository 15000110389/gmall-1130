package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IPUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {
    @Resource
    private JwtProperties jwtProperties;
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }


    @Override
    public GatewayFilter apply(PathConfig config) {
        return (exchange,chain)->{
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            // 1.判断当前请求在不在拦截名单中,不在直接放行
            List<String> pathes = config.pathes;
            String curPath = request.getURI().getPath();
            if (pathes.stream().allMatch(path-> !StringUtils.startsWith(curPath,path))) {
                return chain.filter(exchange);
            }
            //2.获取请求中的token信息(头信息cookie)
            String token = request.getHeaders().getFirst("token");//异步请求,通过头信息获取
            if (StringUtils.isEmpty(token)){    //尝试从cookie中获取(同步)
                MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                if (cookies != null && cookies.containsKey(jwtProperties.getCookieName())) {
                    HttpCookie httpCookie = cookies.getFirst(jwtProperties.getCookieName());
                    token= httpCookie.getValue();
                }
            }


            //3.判断token是否为空,为空则拦截并重定向登录页面
            if (StringUtils.isEmpty(token)){
                return getVoidMono(request, response);
            }
            try {
                //4.解析token,如果出现异常则拦截并重定向登录页面
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                //5.获取载荷中的ip和当前请求的ip比较。如果不一致则拦截并重定向登录页面
                String ip = map.get("ip").toString();
                String curIp = IPUtils.getIpAddressAtGateway(request);
                if (!StringUtils.equals(ip,curIp)){
                    return getVoidMono(request, response);
                }
                //6.为了避免重复解析,通过request头信息把用户信息传递给后续服务
                request.mutate().header("userId",map.get("userId").toString()).build();
                exchange.mutate().request(request).build();
                //7.放行

                return chain.filter(exchange);
            } catch (Exception e) {
                e.printStackTrace();
                return getVoidMono(request, response);
            }
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("pathes");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    private Mono<Void> getVoidMono(ServerHttpRequest request, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
        return response.setComplete();
    }

    @Data
    public static final class PathConfig{
        private List<String> pathes;
    }
}
