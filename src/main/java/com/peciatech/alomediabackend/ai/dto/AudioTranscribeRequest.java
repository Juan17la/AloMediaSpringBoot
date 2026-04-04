package com.peciatech.alomediabackend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AudioTranscribeRequest {

    private String model;
    private String lang;
    private List<String> formats;
}
