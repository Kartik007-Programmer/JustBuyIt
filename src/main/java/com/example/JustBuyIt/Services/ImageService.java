package com.example.JustBuyIt.Services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ImageService {

    private final RestClient restClient = RestClient.create();

    public byte[] downloadImageFromUrl(String ImageUrl) {
        return restClient
                .get()
                .uri(ImageUrl)
                .retrieve()
                .body(byte[].class);
    }
}
