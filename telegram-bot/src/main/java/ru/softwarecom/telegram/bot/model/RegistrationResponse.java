package ru.softwarecom.telegram.bot.model;

import lombok.Data;

@Data
public class RegistrationResponse {
    private String code;
    private String url;
    private String error;
    private String method;
    private Params params;
}
