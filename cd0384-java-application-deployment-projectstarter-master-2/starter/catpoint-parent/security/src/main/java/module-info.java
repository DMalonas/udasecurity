/**
 * @author DMalonas
 */
    module security {
    requires java.desktop;
    requires miglayout;
    requires com.udacity.catpoint.image;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    opens model to com.google.gson;

}