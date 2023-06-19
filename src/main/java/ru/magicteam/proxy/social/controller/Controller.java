package ru.magicteam.proxy.social.controller;

import org.slf4j.Logger;
import ru.magicteam.proxy.social.model.ModelAPI;

public abstract class Controller {

    protected final ModelAPI api;
    protected final Logger logger;

    public Controller(ModelAPI api, Logger logger){
        this.api = api;
        this.logger = logger;
    }

}
