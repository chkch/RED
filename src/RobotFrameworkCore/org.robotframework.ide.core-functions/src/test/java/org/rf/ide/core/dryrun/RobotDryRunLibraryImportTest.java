/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.dryrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport.DryRunLibraryType;

public class RobotDryRunLibraryImportTest {

    @Test
    public void pythonSourceTypeIsResolved() throws Exception {
        assertThat(new RobotDryRunLibraryImport("lib", "source.py", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.PYTHON);
        assertThat(new RobotDryRunLibraryImport("lib", "source.pyc", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.PYTHON);
        assertThat(new RobotDryRunLibraryImport("lib", "source$py.class", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.PYTHON);
    }

    @Test
    public void javaSourceTypeIsResolved() throws Exception {
        assertThat(new RobotDryRunLibraryImport("lib", "source.java", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.JAVA);
        assertThat(new RobotDryRunLibraryImport("lib", "source.jar", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.JAVA);
        assertThat(new RobotDryRunLibraryImport("lib", "source.class", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.JAVA);
    }

    @Test
    public void sourceTypeIsNotResolved() throws Exception {
        assertThat(new RobotDryRunLibraryImport("lib", "", "suite.robot", Arrays.asList()).getType())
                .isEqualTo(DryRunLibraryType.UNKNOWN);
    }
}
