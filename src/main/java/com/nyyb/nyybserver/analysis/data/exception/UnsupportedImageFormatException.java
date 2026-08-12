package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class UnsupportedImageFormatException extends GlobalException {
    public UnsupportedImageFormatException() {
        super(ErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
}
