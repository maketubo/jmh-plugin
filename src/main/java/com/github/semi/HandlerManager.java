package com.github.semi;


import com.github.semi.handler.FixReturnSemiHandler;
import com.github.semi.handler.FixBlockSemiHandler;

import java.util.List;

import java.util.ArrayList;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName EnterManager
 * @description
 * @date 2020/5/10 15:24
 * @since JDK 1.8
 */
public class HandlerManager {

    private static List<ISemiHandler> handlers = new ArrayList<>();

    static  {
        handlers.add(new FixReturnSemiHandler());
        handlers.add(new FixBlockSemiHandler());
    }

    public void addHandler(ISemiHandler enterHandler) {
        handlers.add(enterHandler);
    }

    boolean onEnter(SemiEvent event){
        return false;
    }

    public static List<ISemiHandler> getHandlers() {
        return handlers;
    }
}
