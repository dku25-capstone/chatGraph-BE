package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.security.CustomUserDetails;

public abstract class BaseController {
    protected String getUserId(CustomUserDetails userDetails) {
        return userDetails.getUserId();
    }
}
