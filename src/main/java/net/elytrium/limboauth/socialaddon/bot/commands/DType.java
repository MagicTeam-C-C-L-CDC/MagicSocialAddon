package net.elytrium.limboauth.socialaddon.bot.commands;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum DType {
    PING,
    MODAL,
    BUILD,
    GENERATE_TYPE,

    GENERATE_GOOGLE_FORM,
    CREATE_GOOGLE_FORM_BUTTON;

    public final String name;

    DType() {
        this.name = this.toString().toLowerCase();
    }

    public static DType fromString(String value){
        Optional<DType> type = Arrays.stream(DType.values()).filter(x -> Objects.equals(x.name, value)).findFirst();
        if(type.isEmpty())
            throw new NullPointerException();
        else return type.get();
    }
}
