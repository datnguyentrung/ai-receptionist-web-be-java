package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CoreErrorCode implements ErrorCode {
    BRANCH_NOT_FOUND(
            "BRANCH_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Branch not found",
            "Branch not found"),
    POSITION_NOT_FOUND("POSITION_NOT_FOUND", HttpStatus.NOT_FOUND, "Position not found", "Position not found"),
    PERSON_NOT_FOUND("PERSON_NOT_FOUND", HttpStatus.NOT_FOUND, "Person not found", "Person not found"),
    USER_PERSON_NOT_FOUND("USER_PERSON_NOT_FOUND", HttpStatus.NOT_FOUND, "User person not found",
            "User person not found"),
    POSITION_CODE_ALREADY_EXISTS("POSITION_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "Position code already exists", "Position code already exists"),
    NATIONAL_CODE_ALREADY_EXISTS("NATIONAL_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "National code already exists", "National code already exists"),
    PERSON_CODE_ALREADY_EXISTS("PERSON_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "Person code already exists", "Person code already exists"),
    PERSON_CODE_POLICY_VIOLATION("PERSON_CODE_POLICY_VIOLATION", HttpStatus.UNPROCESSABLE_CONTENT,
            "Person code policy violation",
            "Person code must start with VQ_ (student) or VQT_ (system employee)"),
    INVALID_IDENTIFICATION_REQUEST("INVALID_IDENTIFICATION_REQUEST", HttpStatus.BAD_REQUEST,
            "Invalid identification request", "Identification requires a face image or person code"),
    INVALID_IMAGE_FILE("INVALID_IMAGE_FILE", HttpStatus.BAD_REQUEST, "Invalid image file",
            "Image file is invalid"),
    EMPTY_IMAGE_FILE("EMPTY_IMAGE_FILE", HttpStatus.BAD_REQUEST, "Empty image file",
            "Image file must not be empty"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, "File too large",
            "File exceeds the allowed size"),
    UNSUPPORTED_IMAGE_TYPE("UNSUPPORTED_IMAGE_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported image type", "Only JPEG, PNG and WebP images are supported"),
    IMAGE_DECODE_FAILED("IMAGE_DECODE_FAILED", HttpStatus.UNPROCESSABLE_ENTITY,
            "Image decode failed", "Image could not be decoded"),
    FACE_NOT_DETECTED("FACE_NOT_DETECTED", HttpStatus.UNPROCESSABLE_ENTITY,
            "Face not detected", "No face was detected in the image"),
    MULTIPLE_FACES_DETECTED("MULTIPLE_FACES_DETECTED", HttpStatus.UNPROCESSABLE_ENTITY,
            "Multiple faces detected", "Exactly one face is required"),
    FACE_EMBEDDING_FAILED("FACE_EMBEDDING_FAILED", HttpStatus.UNPROCESSABLE_ENTITY,
            "Face embedding failed", "Could not generate face embedding"),
    INVALID_EMBEDDING("INVALID_EMBEDDING", HttpStatus.UNPROCESSABLE_ENTITY,
            "Invalid embedding", "Face embedding is invalid"),
    MODEL_NOT_INITIALIZED("MODEL_NOT_INITIALIZED", HttpStatus.SERVICE_UNAVAILABLE,
            "Face model not initialized", "Face model is not initialized"),
    PYTHON_BACKEND_UNAVAILABLE("PYTHON_BACKEND_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
            "Python backend unavailable", "Python backend is temporarily unavailable"),
    PYTHON_BACKEND_ERROR("PYTHON_BACKEND_ERROR", HttpStatus.BAD_GATEWAY,
            "Python backend error", "Python backend request failed"),
    FACE_NOT_MATCHED("FACE_NOT_MATCHED", HttpStatus.NOT_FOUND,
            "Face not matched", "No registered person matched this face"),
    FACE_CHECK_IN_PERSON_TYPE_INVALID("FACE_CHECK_IN_PERSON_TYPE_INVALID", HttpStatus.CONFLICT,
            "Face check-in person type invalid", "Person cannot be resolved for face check-in"),
    SUPABASE_STORAGE_NOT_CONFIGURED("SUPABASE_STORAGE_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE,
            "Supabase storage not configured", "Supabase storage is not configured"),
    SUPABASE_STORAGE_UNAVAILABLE("SUPABASE_STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
            "Supabase storage unavailable", "Supabase storage is temporarily unavailable"),
    SUPABASE_STORAGE_UPLOAD_FAILED("SUPABASE_STORAGE_UPLOAD_FAILED", HttpStatus.BAD_GATEWAY,
            "Supabase storage upload failed", "Could not upload face image"),
    SUPABASE_STORAGE_DELETE_FAILED("SUPABASE_STORAGE_DELETE_FAILED", HttpStatus.BAD_GATEWAY,
            "Supabase storage delete failed", "Could not delete face image");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
