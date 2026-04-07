package com.fyp.qa.healing;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;

public class HealerClient {

    private final String apiUrl;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public HealerClient(String apiUrl) {
        this.apiUrl = apiUrl;
        this.mapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(20))
                .callTimeout(Duration.ofSeconds(20))
                .build();
    }

    public HealDTO.HealResponse heal(HealDTO.HealRequest req) throws IOException {
        long start = System.currentTimeMillis();

        String bodyJson = mapper.writeValueAsString(req);

        // append /heal path if not already present
        String url = apiUrl.endsWith("/heal") ? apiUrl : apiUrl.replaceAll("/+$", "") + "/heal";

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyJson, JSON))
                .build();

        // retry once on timeout or 5xx — transient API blips should not abort the heal
        IOException lastEx = null;
        Response response = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    lastEx = null;
                    break;
                }
                // 5xx — retry once
                int code = response.code();
                if (code >= 500 && attempt < 2) {
                    response.close();
                    response = null;
                    Thread.sleep(500);
                    continue;
                }
                break; // 4xx or second 5xx — don't retry
            } catch (java.net.SocketTimeoutException | java.net.ConnectException ex) {
                lastEx = new IOException("Attempt " + attempt + " failed: " + ex.getMessage(), ex);
                if (attempt < 2) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Heal interrupted", ie);
            }
        }

        if (lastEx != null) {
            long ms = System.currentTimeMillis() - start;
            throw new IOException("Healer API unreachable after 2 attempts: url=" + url + " elapsedMs=" + ms, lastEx);
        }

        if (response == null) {
            throw new IOException("Healer API: no response after retries | url=" + url);
        }

        try (Response r = response) {
            long ms = System.currentTimeMillis() - start;
            int code = r.code();
            String respBody = r.body() != null ? r.body().string() : "";

            if (!r.isSuccessful()) {
                throw new IOException("Healer API failed: " + code + " " + r.message() +
                        " | url=" + url +
                        " | elapsedMs=" + ms +
                        " | body=" + truncate(respBody, 800));
            }

            try {
                HealDTO.HealResponse parsed = mapper.readValue(respBody, HealDTO.HealResponse.class);
                System.out.println("HEAL parsed response = " + parsed);
                return parsed;
            } catch (Exception pe) {
                throw new IOException("Healer API parse failed" +
                        " | url=" + url +
                        " | elapsedMs=" + ms +
                        " | body=" + truncate(respBody, 800), pe);
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
