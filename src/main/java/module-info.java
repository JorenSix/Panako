/**
 * Defines the panako module info
 */
module panako {
    requires java.logging;
    requires java.prefs;
    requires java.desktop;

    requires TarsosDSP.jvm;
    requires TarsosDSP.core;
    requires JGaborator;

    requires org.reflections;

    requires lmdbjava;

    exports be.panako.strategy;
    exports be.panako.util;
    exports be.panako.strategy.panako;
    exports be.panako.strategy.olaf;
    exports be.panako.strategy.olaf.storage;
}