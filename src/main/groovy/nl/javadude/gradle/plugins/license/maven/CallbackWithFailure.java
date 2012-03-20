package nl.javadude.gradle.plugins.license.maven;

import java.io.File;
import java.util.Collection;

import com.google.code.mojo.license.Callback;

public interface CallbackWithFailure extends Callback {
    boolean hadFailure();
    Collection<File> getAffected();
}
