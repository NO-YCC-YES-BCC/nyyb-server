package com.nyyb.nyybserver.common.s3;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class ImageUploadException extends GlobalException {
    public ImageUploadException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
