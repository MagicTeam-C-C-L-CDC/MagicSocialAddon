package ru.magicteam.proxy.social.controller.google;

import java.util.Collection;

public record GoogleFormResponse(Collection<GoogleFormAnswer> fields) {
}

record GoogleFormAnswer(String name, String value){}
