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
        String url = apiUrl + "/heal";

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(bodyJson, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            long ms = System.currentTimeMillis() - start;

            int code = response.code();
            String respBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // ✅ THIS is the missing part: FastAPI error details are in the body (422 especially)
                throw new IOException("Healer API failed: " + code + " " + response.message() +
                        " | url=" + url +
                        " | elapsedMs=" + ms +
                        " | body=" + truncate(respBody, 800));
            }

            // Optional: debug log (if you want)
            // logger.info("Healer API OK url={} elapsedMs={} body={}", url, ms, truncate(respBody, 300));

            return mapper.readValue(respBody, HealDTO.HealResponse.class);

        } catch (java.net.SocketTimeoutException te) {
            long ms = System.currentTimeMillis() - start;
            throw new IOException("Healer API timeout: url=" + url + " elapsedMs=" + ms, te);
        } catch (java.net.ConnectException ce) {
            long ms = System.currentTimeMillis() - start;
            throw new IOException("Healer API connection failed: url=" + url + " elapsedMs=" + ms, ce);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }

}
