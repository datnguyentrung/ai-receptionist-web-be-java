package com.dat.ai_receptionist_web.dto.Sparring;

/** URL upload có hạn và objectPath phải được gửi lại qua upload-complete. */
public record SparringPracticeUploadResponse(String uploadUrl, String objectPath, long maxBytes) { }
