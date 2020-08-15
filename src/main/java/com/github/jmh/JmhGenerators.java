package com.github.jmh;

import com.intellij.lang.LanguageExtension;

/**
 * @author maketubo
 * @version 1.0
 * @ClassName JmhTestGenerators
 * @description
 * @date 2020/5/28 0:10
 * @since JDK 1.8
 */
public class JmhGenerators extends LanguageExtension<JmhGenerator> {
    public static final JmhGenerators INSTANCE = new JmhGenerators();

    private JmhGenerators() {
        super("com.intellij.jmhGenerator", new JmhGenerator());
    }
}
