package com.nyyb.nyybserver.analysis.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class AnalysisNotFoundException extends GlobalException {
    public AnalysisNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
