package com.heaterworkshop.infrastructure.web;

import jakarta.validation.constraints.NotNull;

public record ApproveWorkOrderRequest(@NotNull Boolean partsAvailable) { }
