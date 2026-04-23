package com.peciatech.alomediabackend.project.media;

public record StorageBinaryResource(byte[] data, String contentType, String fileName) {
}
