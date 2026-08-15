package com.nyyb.nyybserver.routine.data.exception;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.response.GlobalException;

public class RoutineNotFoundException extends GlobalException {
    public RoutineNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}
