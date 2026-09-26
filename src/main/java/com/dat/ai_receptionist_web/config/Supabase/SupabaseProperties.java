package com.dat.ai_receptionist_web.config.Supabase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {
    private String url;
    private String serviceRoleKey;
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Storage {
        private String faceImageBucket = "face-images";
        private long maxFileSize = 5_242_880L;
        private long signedUrlTtlSeconds = 3_600L;
    }
}
