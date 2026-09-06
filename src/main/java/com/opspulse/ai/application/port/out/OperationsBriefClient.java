package com.opspulse.ai.application.port.out;

import com.opspulse.ai.domain.BriefRequest;
import com.opspulse.ai.domain.BriefResponse;

public interface OperationsBriefClient {
    BriefResponse generate(BriefRequest request);
}
