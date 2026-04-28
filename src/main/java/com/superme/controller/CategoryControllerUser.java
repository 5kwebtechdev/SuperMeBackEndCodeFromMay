//package com.superme.controller;
//
//import org.springframework.web.bind.annotation.*;
//import java.util.*;
//
//@RestController
//@RequestMapping("/user/categories")
//@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
//public class CategoryControllerUser {
//
//    private static final String S3_BASE_URL = "https://your-bucket.s3.amazonaws.com/journal/";
//
//    @GetMapping("/options")
//    public Map<String, List<Map<String, String>>> getCategoryOptions() {
//        return CategoryController.options;
//    }
//
//
//    /* -------------------------------------------------------------------- */
//    /* Utility to build a small JSON object                                 */
//    /* -------------------------------------------------------------------- */
//    private static Map<String, String> icon(String value, String key) {
//        return Map.of(
//                "value", value,
//                "icon",  S3_BASE_URL + key
//        );
//    }
//}
