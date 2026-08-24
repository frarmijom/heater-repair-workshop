package com.heaterworkshop.application.port;

import com.heaterworkshop.domain.valueobject.CustomerContact;

public interface CustomerNotifier {
    void notify(CustomerContact destination, String message);
}
