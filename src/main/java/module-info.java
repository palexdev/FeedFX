import io.inverno.core.annotation.Module;

@Module(
    className = "io.github.palexdev.feedfx.MainModule",
    sourcePackage = "io.github.palexdev.feedfx"
)
module FeedFX {
    //***** Dependencies *****//
    // JavaFX/UI
    requires afx.backend;
    requires mfx.components;
    requires VirtualizedFX;
    requires rectcut;

    // DI
    requires io.inverno.core;

    // Logging
    requires org.tinylog.api;
    requires org.tinylog.api.slf4j;
    requires org.apache.logging.log4j.to.slf4j;

    // Misc
    requires com.twelvemonkeys.imageio.bmp;
    requires com.twelvemonkeys.imageio.webp;
    requires ImCache;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.rometools.rome;
    requires com.rometools.rome.modules;

    //***** Exports *****//
    exports io.github.palexdev.feedfx;

    exports  io.github.palexdev.feedfx.di;

    exports  io.github.palexdev.feedfx.events;

    exports io.github.palexdev.feedfx.model;

    exports io.github.palexdev.feedfx.theming;

    exports io.github.palexdev.feedfx.ui;
    exports io.github.palexdev.feedfx.ui.components;
    exports io.github.palexdev.feedfx.ui.components.dialogs;
    exports io.github.palexdev.feedfx.ui.components.selection;
    exports io.github.palexdev.feedfx.ui.controllers;

    exports io.github.palexdev.feedfx.utils;

    //***** Opens *****//
    opens io.github.palexdev.feedfx.ui;
    opens io.github.palexdev.feedfx.ui.components;
    opens io.github.palexdev.feedfx.ui.controllers;
    opens io.github.palexdev.feedfx.ui.components.dialogs;
}