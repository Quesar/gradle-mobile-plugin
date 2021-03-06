/*
 * @(#)XcodeTasks.java
 *
 * Copyright C.T.Co Ltd, 15/25 Jurkalnes Street, Riga LV-1046, Latvia. All rights reserved.
 */

package lv.ctco.scm.gradle.xcode;

import lv.ctco.scm.gradle.TaskGroup;

import org.apache.commons.lang3.StringUtils;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;

public final class XcodeTasks {

    private XcodeTasks() {}

    static Task getOrCreateCleanTask(Project project) {
        String taskName = "clean";
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            CleanTask newTask = project.getTasks().create(taskName, CleanTask.class);
            newTask.setGroup(TaskGroup.UTILITY.getLabel());
            newTask.setDescription("Reverts applied profiling and deletes known build directories");
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateProjectInfoTask(Project project, XcodeConfiguration xcodeConfiguration) {
        String taskName = "projectInfo";
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            ProjectInfoTask newTask = project.getTasks().create(taskName, ProjectInfoTask.class);
            newTask.setGroup(TaskGroup.UTILITY.getLabel());
            newTask.setDescription("Prints project's configuration information");
            newTask.setXcodeConfiguration(xcodeConfiguration);
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateRestoreDependenciesTask(Project project) {
        String taskName = "restoreDependencies";
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            DependencyRestoreTask newTask = project.getTasks().create(taskName, DependencyRestoreTask.class);
            newTask.setGroup(TaskGroup.BUILD.getLabel());
            newTask.setDescription("Restores 'xdeps' configuration's dependencies to 'Libraries' folder");
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateBuildTask(Project project) {
        String taskName = "build";
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            DefaultTask newTask = project.getTasks().create(taskName, DefaultTask.class);
            newTask.setGroup(TaskGroup.BUILD.getLabel());
            newTask.setDescription("Builds all project's environments");
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateBuildEnvTask(Project project, Environment env) {
        String taskName = "build"+ StringUtils.capitalize(env.getName());
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            BuildTask newTask = project.getTasks().create(taskName, BuildTask.class);
            newTask.setGroup(TaskGroup.BUILD.getLabel());
            newTask.setDescription("Builds '"+env.getName()+"' environment with '"+env.getTarget()+"' target");
            newTask.setEnv(env);
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateUpdateVersionTask(Project project, Environment env) {
        String taskName = "updateVersion"+StringUtils.capitalize(env.getName());
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            UpdateVersionIosTask newTask = project.getTasks().create(taskName, UpdateVersionIosTask.class);
            // Do not set group so the task will be nicely sorted in the 'tasks -all' output
            newTask.setDescription("Updates app version for '"+env.getName()+"' environment");
            newTask.setTargetName(env.getTarget());
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateRevertProfileTask(Project project, String envName) {
        String taskName = "revertProfile"+StringUtils.capitalize(envName);
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            RevertProfileTask newTask = project.getTasks().create(taskName, RevertProfileTask.class);
            newTask.setGroup(TaskGroup.UTILITY.getLabel());
            newTask.setDescription("Reverts applied profiling");
            return newTask;
        } else {
            return existingTask;
        }
    }

    static Task getOrCreateRunUnitTestsTask(Project project, String schemeName) {
        String taskName = "runUnitTests";
        Task existingTask = project.getTasks().findByName(taskName);
        if (existingTask == null) {
            UnitTestingTask newTask = project.getTasks().create(taskName, UnitTestingTask.class);
            newTask.setGroup(TaskGroup.TESTS.getLabel());
            newTask.setDescription("Runs unit tests using a defined scheme");
            newTask.setSchemeName(schemeName);
            return newTask;
        } else {
            return existingTask;
        }
    }

}
