package com.shotty.shotty.global.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shotty.shotty.global.common.dto.ResponseDto;
import com.shotty.shotty.global.util.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class LoginCheckFilter implements Filter {
    private final JwtProvider jwtProvider;
    private static final List<String> whiteListSwagger = Arrays.asList(
            "/swagger-ui","/v3/api-docs"
    );
    private final Map<String, Set<String>> whiteList = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 화이트리스트 초기화
        whiteList.put("/api/auth/register", new HashSet<>(Arrays.asList("POST", "OPTIONS")));
        whiteList.put("/api/auth/login", new HashSet<>(Arrays.asList("POST", "OPTIONS")));
        whiteList.put("/api/influencers", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
        whiteList.put("/api/influencers/{id}", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
        whiteList.put("/api/influencers/niches", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
        whiteList.put("/api/users/{id}", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
        whiteList.put("/api/posts", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
        whiteList.put("/api/posts/{postId}", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
        whiteList.put("/api/applies/kinds", new HashSet<>(Arrays.asList("GET", "OPTIONS")));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        boolean isSwagger = whiteListSwagger.stream()
                .anyMatch(path::startsWith);

        if( isSwagger ){
            filterChain.doFilter(request, response);
            log.info("화이트리스트: path= {} method= {}", path,method);
            return;
        }


        boolean isAllowed = whiteList.entrySet().stream()
                .anyMatch(entry -> matchesPath(entry.getKey(), path) &&
                        entry.getValue().contains(method)) || method.equals("OPTIONS");

        if( isAllowed ){
            filterChain.doFilter(request, response);
            log.info("화이트리스트: path= {} method= {}", path,method);
            return;
        }
        //토큰 여부 확인
        String accessToken = getAccessToken(httpRequest);
        
        log.info("토큰 여부 확인: Access token: {},path= {}", accessToken,path);
        if(accessToken == null ) {
            setCorsHeader(httpRequest, httpResponse);

            setResponse(httpResponse,HttpServletResponse.SC_UNAUTHORIZED,4010,"missing header");
            return;
        }

        //accessToken 검증
        log.info("accessToken 검증");
        try {
            jwtProvider.validateToken(accessToken);
        }catch (ExpiredJwtException  e) {
            setCorsHeader(httpRequest, httpResponse);

            setResponse(httpResponse,HttpServletResponse.SC_BAD_REQUEST,4002,"Expired accessToken");
            return;
        }catch (Exception e) {
            setCorsHeader(httpRequest, httpResponse);

            setResponse(httpResponse,HttpServletResponse.SC_BAD_REQUEST,4003,"Invalid access token");
            return;
        }

        //권한 확인### {Id,userRole}


        filterChain.doFilter(request, response);
    }

    private static void setCorsHeader(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String origin = httpRequest.getHeader("Origin");

        if (origin != null && (
                origin.equals("http://localhost:5173") ||
                        origin.equals("http://localhost:8080") ||
                        origin.equals("https://shotty-one.vercel.app") ||
                        origin.equals("https://shotty-app.shop"))) {

            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "*");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        }
    }

    private boolean matchesPath(String pattern, String path) {
        // 패턴의 '{id}' 부분을 실제 값으로 대체하여 경로 매칭
        String regex = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
        return path.matches(regex);
    }

    private void setResponse(HttpServletResponse httpResponse,int httpStatus,int code,String statusMsg) throws IOException {
        httpResponse.setStatus(httpStatus);
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("utf-8");
        ResponseDto<Null> responseDto = new ResponseDto<>(
                code,
                statusMsg,
                null
        );
        ObjectMapper objectMapper = new ObjectMapper();
        String responseJson = objectMapper.writeValueAsString(responseDto);
        httpResponse.getWriter().write(responseJson);
    }

    private String getAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
