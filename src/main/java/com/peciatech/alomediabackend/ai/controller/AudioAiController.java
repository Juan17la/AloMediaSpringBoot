package com.peciatech.alomediabackend.ai.controller;

import com.peciatech.alomediabackend.ai.dto.AudioCleanRequest;
import com.peciatech.alomediabackend.ai.dto.AudioTranscribeRequest;
import com.peciatech.alomediabackend.ai.service.AudioAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ai/audio")
@RequiredArgsConstructor
public class AudioAiController {

    private final AudioAiService audioAiService;

    @PostMapping(value = "/clean", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> cleanAudio(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "backend", required = false) String backend,
            @RequestPart(value = "stationary", required = false) String stationary,
            @RequestPart(value = "targetSr", required = false) String targetSr) {

        AudioCleanRequest request = new AudioCleanRequest();
        request.setBackend(backend);
        if (stationary != null) {
            request.setStationary(Boolean.parseBoolean(stationary));
        }
        if (targetSr != null) {
            request.setTargetSr(Integer.parseInt(targetSr.trim()));
        }

        return audioAiService.cleanAudio(file, request);
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> transcribeAudio(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "model", required = false) String model,
            @RequestPart(value = "lang", required = false) String lang,
            @RequestPart(value = "formats", required = false) List<String> formats) {

        AudioTranscribeRequest request = new AudioTranscribeRequest();
        request.setModel(model);
        request.setLang(lang);
        request.setFormats(formats);

        return audioAiService.transcribeAudio(file, request);
    }
}
