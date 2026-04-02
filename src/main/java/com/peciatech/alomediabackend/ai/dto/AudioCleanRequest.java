package com.peciatech.alomediabackend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AudioCleanRequest {

    private String backend;
    private Boolean stationary;
    private Integer targetSr;
}
