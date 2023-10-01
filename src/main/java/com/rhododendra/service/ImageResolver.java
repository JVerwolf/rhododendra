package com.rhododendra.service;

import java.util.List;

public class ImageResolver {
    public static String resolveImagePath(String img){
        return "/img/" + img;
    }
    public static List<String>  resolveImages(List<String> img){
        return img.stream().map(ImageResolver::resolveImagePath).toList();
    }
}
