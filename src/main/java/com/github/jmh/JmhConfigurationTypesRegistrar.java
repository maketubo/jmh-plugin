package com.github.jmh;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;

import java.util.Arrays;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName JmhConfigurationTypesRegistrar
 * @description
 * @date 2020/7/29 10:29
 * @since JDK 1.8
 */
public class JmhConfigurationTypesRegistrar implements ApplicationInitializedListener {
    @Override
    public void componentsInitialized() {
        ((ExtensionPointImpl<JmhFramework>)JmhFramework.EXTENSION_NAME.getPoint(null))
                .registerExtensions(Arrays.asList(
                        new DefaultJmhFramework()
                ));
    }
}
