package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class OcrApiException extends GlobalException {
    public OcrApiException() {
        super(ErrorCode.OCR_API_FAILED);
    }
}
