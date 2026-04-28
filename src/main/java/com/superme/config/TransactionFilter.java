//package com.mindfull.config;
//
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//import java.io.IOException;
//
//@Component
//@Order(1) // ✅ CHANGED: Run BEFORE JwtAuthFilter to handle CORS and basic validation
//public class TransactionFilter extends OncePerRequestFilter {
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
//        String uri = request.getRequestURI();
//        String method = request.getMethod();
//
//        // ✅ IMPROVED: Always allow OPTIONS requests
//        if ("OPTIONS".equalsIgnoreCase(method)) {
//            return true;
//        }
//
//        return false;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//            HttpServletResponse response,
//            FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String uri = request.getRequestURI();
//        String method = request.getMethod();
//
//        // ✅ IMPROVED: Set CORS Headers for ALL requests (including errors)
//        response.setHeader("Access-Control-Allow-Origin", "*");
//        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE, PATCH");
//        response.setHeader("Access-Control-Max-Age", "3600");
//        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
//
//        // ✅ IMPROVED: Handle OPTIONS requests early and return
//        if ("OPTIONS".equalsIgnoreCase(method)) {
//            response.setStatus(HttpServletResponse.SC_OK);
//            return;
//        }
//
//        // Forward other requests down the filter chain!
//        filterChain.doFilter(request, response);
//    }
//}
