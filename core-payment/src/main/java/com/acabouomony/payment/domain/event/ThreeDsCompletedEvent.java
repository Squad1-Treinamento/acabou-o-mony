package com.acabouomony.payment.domain.event;

import java.util.UUID;

public record ThreeDsCompletedEvent(UUID txId, boolean approved) {}
