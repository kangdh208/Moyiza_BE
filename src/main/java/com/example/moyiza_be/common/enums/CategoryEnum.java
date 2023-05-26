package com.example.moyiza_be.common.enums;

import jdk.jfr.Category;

public enum CategoryEnum {
    SPORTS("스포츠"),
    FITNESS("운동"),
    TRAVEL("여행"),

    CULTURE("문화"),

    ART("예술"),

    ACTIVITY("액티비티"),

    FOOD("음식"),

    SELFDEV("자기계발"),

    HOBBY("취미"),

    DATE("연애");



    private final String category;

    CategoryEnum(String category) {
        this.category = category;
    }

    public String getCategory(){return this.category;}

    public static CategoryEnum fromString(String category) {
        for (CategoryEnum categoryEnum : CategoryEnum.values()) {
            if (categoryEnum.getCategory().equalsIgnoreCase(category)) {
                return categoryEnum;
            }
        }
        throw new IllegalArgumentException(String.format("%s에 해당하는 카테고리를 찾을 수 없습니다",category));
    }
}
