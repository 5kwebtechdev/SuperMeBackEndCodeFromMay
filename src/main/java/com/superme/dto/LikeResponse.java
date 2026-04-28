package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeResponse {
    private boolean liked;      // true = liked, false = unliked
    private int totalLikes;
}
