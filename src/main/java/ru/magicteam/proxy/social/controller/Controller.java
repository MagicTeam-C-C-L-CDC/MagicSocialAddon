package ru.magicteam.proxy.social.controller;

import ru.magicteam.proxy.social.model.ModelAPI;

public abstract class Controller {

    protected ModelAPI api;

    public Controller(ModelAPI api){
        this.api = api;
    }

}
