package com.lgc.gitlabtool.git.services;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;
import com.lgc.gitlabtool.git.connections.RESTConnector;
import com.lgc.gitlabtool.git.connections.token.CurrentUser;
import com.lgc.gitlabtool.git.entities.Group;
import com.lgc.gitlabtool.git.entities.Project;
import com.lgc.gitlabtool.git.util.JSONParser;
import com.lgc.gitlabtool.git.util.PathUtilities;

public class ProjectServiceImpl implements ProjectService {
    private RESTConnector _connector;

    private static String privateTokenKey;
    private static String privateTokenValue;

    private static final String GROUP_DOESNT_HAVE_PROJECTS_MESSAGE = "The group has no projects.";
    private static final String PREFIX_SUCCESSFUL_LOAD = " group loaded successful";

    private static final Logger _logger = LogManager.getLogger(ProjectServiceImpl.class);

    public ProjectServiceImpl(RESTConnector connector) {
        setConnector(connector);
    }

    @Override
    public Collection<Project> getProjects(Group group) {
        privateTokenValue = CurrentUser.getInstance().getPrivateTokenValue();
        privateTokenKey = CurrentUser.getInstance().getPrivateTokenKey();
        if (privateTokenValue != null) {
            String sendString = "/groups/" + group.getId() + "/projects";
            HashMap<String, String> header = new HashMap<>();
            header.put(privateTokenKey, privateTokenValue);
            Object jsonProjects = getConnector().sendGet(sendString, null, header);

            return JSONParser.parseToCollectionObjects(jsonProjects, new TypeToken<List<Project>>() {
            }.getType());
        }
        return null;
    }

    private RESTConnector getConnector() {
        return _connector;
    }

    private void setConnector(RESTConnector connector) {
        _connector = connector;
    }

    @Override
    public Collection<Project> loadProject(Group group) {
        Collection<Project> projects = getProjects(group);
        if (projects == null || projects.isEmpty()) {
            _logger.debug(GROUP_DOESNT_HAVE_PROJECTS_MESSAGE);
            return Collections.emptyList();
        }
        String successMessage = "The projects of " + group.getName() + PREFIX_SUCCESSFUL_LOAD;
        Path path = Paths.get(group.getPathToClonedGroup());
        Collection<String> projectsName = PathUtilities.getFolders(path);
        if (projectsName.isEmpty()) {
            _logger.debug(successMessage);
            return projects;
        }
        projects.stream()
                .filter(project -> projectsName.contains(project.getName()))
                .forEach((project) -> updateProjectStatus(project, group.getPathToClonedGroup()));
        _logger.debug(successMessage);
        return projects;
    }

    private void updateProjectStatus(Project project, String pathGroup) {
        project.setClonedStatus(true);
        project.setPathToClonedProject(pathGroup + File.separator + project.getName());
        ProjectTypeService typeService = (ProjectTypeService) ServiceProvider.getInstance()
                .getService(ProjectTypeService.class.getName());
        project.setProjectType(typeService.getProjectType(project));
    }
}