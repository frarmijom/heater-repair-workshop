package com.heaterworkshop.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RepairEstimate {

    private double total = 0.0;
    private final List<String> requiredParts = new ArrayList<>();

    public double getTotal() {
        return total;
    }

    public List<String> getRequiredParts() {
        return Collections.unmodifiableList(requiredParts);
    }

    public int getTotalParts() {
        return requiredParts.size();
    }
}
