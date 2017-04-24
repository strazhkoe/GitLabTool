package com.lgc.solutiontool.git.services;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;

import com.lgc.solutiontool.git.entities.Project;

/**
 * Service for changes of a group's pom.xml.
 *
 * @author Lyudmila Lyska
 */
public class PomXMLServiceImpl implements PomXMLService {

    private static final String RELEASE_NAME_KEY = "releaseName";
    private static final String REPOSITORY_LAYOUT = "p2";

    @Override
    public void changeParentVersion(Collection<Project> projects, String newVersion) {
        if (projects == null || !isValidString(newVersion)) {
            System.err.println("Not valid data was submitted. Cannot modify the pom.xml files."); // TODO replaced by log
            return;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            PomXMLModel model = getModel(project);
            if (changeParentVersion(model, newVersion)) {
                model.writeToFile();
                System.out.println("The pom.xml file was changed successfully."); // TODO replaced by log
            } else {
                System.out.println("Error in changing the pom.xml file."); // TODO replaced by log
            }
        }
    }

    @Override
    public void changeGroupName(Collection<Project> projects, String oldName, String newName) {
        if (projects == null || !isValidString(newName) || !isValidString(oldName)) {
            System.err.println("Not valid data was submitted. Cannot modify the pom.xml files."); // TODO replaced by log
            return;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            PomXMLModel model = getModel(project);
            if (changeGroupName(model, oldName, newName)) {
                model.writeToFile();
                System.out.println("The pom.xml file was changed successfully."); // TODO replaced by log
            } else {
                System.out.println("Error in changing the pom.xml file."); // TODO replaced by log
            }
        }
    }

    @Override
    public void changeReleaseName(Collection<Project> projects, String newName) {
        if (projects == null || !isValidString(newName)) {
            System.err.println("Not valid data was submitted. Cannot modify the pom.xml files."); // TODO replaced by log
            return;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            PomXMLModel model = getModel(project);
            if (changeReleaseName(model, newName)) {
                model.writeToFile();
                System.out.println("The pom.xml file was changed successfully."); // TODO replaced by log
            } else {
                System.out.println("Error in changing the pom.xml file."); // TODO replaced by log
            }
        }
    }

    @Override
    public void addRepository(Collection<Project> projects, String id, String url) {
        if (projects == null || !isValidString(id) || !isValidString(url)) {
            System.err.println("Not valid data was submitted. Cannot modify the pom.xml files."); // TODO replaced by log
            return;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            PomXMLModel model = getModel(project);
            if(addRepository(model, id, url)) {
                model.writeToFile();
                System.out.println("The pom.xml file was changed successfully."); // TODO replaced by log
            } else {
                System.out.println("Error in changing the pom.xml file."); // TODO replaced by log
            }
        }
    }

    @Override
    public void removeRepository(Collection<Project> projects, String id) {
        if (projects == null || !isValidString(id)) {
            System.err.println("Not valid data was submitted. Cannot modify the pom.xml files."); // TODO replaced by log
            return;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            PomXMLModel model = getModel(project);
            if (removeRepository(model, id)) {
                model.writeToFile();
                System.out.println("The pom.xml file was changed successfully."); // TODO replaced by log
            } else {
                System.out.println("Error in changing the pom.xml file."); // TODO replaced by log
            }
        }
    }

    @Override
    public void modifyRepository(Collection<Project> projects, String oldId, String newId, String newUrl) {
        if (projects == null || !isValidString(oldId) || !isValidString(newId) || !isValidString(newUrl)) {
            System.err.println("Not valid data was submitted. Cannot modify the pom.xml files."); // TODO replaced by log
            return;
        }
        for (Project project : projects) {
            if (project == null) {
                continue;
            }
            PomXMLModel model = getModel(project);
            List<Repository> rep = model.getModelFile().get().getRepositories();
            if (modifyRepository(rep, oldId, newId, newUrl)) {
                model.writeToFile();
                System.out.println("The pom.xml file was changed successfully."); // TODO replaced by log
            } else {
                System.out.println("Error in changing the pom.xml file."); // TODO replaced by log
            }
        }
    }

    private boolean changeParentVersion(PomXMLModel pomMng, String newVersion) {
        Optional<Model> model = pomMng.getModelFile();
        if (model.isPresent()) {
            Parent parent = model.get().getParent();
            if (parent != null) {
                parent.setVersion(newVersion);
                return true;
            }
        }
        return false;
    }

    private boolean changeGroupName(PomXMLModel pomMng, String oldName, String newName) {
        Optional<Model> optModel = pomMng.getModelFile();
        if (!optModel.isPresent()) {
            return false;
        }
        Model xmlModel = optModel.get();
        boolean properties = changeGroupNameInProperties(xmlModel.getProperties(), oldName, newName);
        boolean repositories = changeGroupNameInRepositories(xmlModel.getRepositories(), oldName, newName);
        boolean scm = changeGroupNameInScm(xmlModel.getScm(), oldName, newName);
        return properties | repositories | scm;
    }

    private boolean changeReleaseName(PomXMLModel pomMng, String newName) {
        Optional<Model> optModel = pomMng.getModelFile();
        if (optModel.isPresent()) {
            Properties pr = optModel.get().getProperties();
            if (pr != null) {
                pr.put(RELEASE_NAME_KEY, newName);
                return true;
            }
        }
        return false;
    }

    private boolean changeGroupNameInRepositories(List<Repository> reps, String oldName, String newName) {
        if (reps == null) {
            return false;
        }
        String regex = "(?i)(" + oldName + ")([\\-\\/])(?<=.)";
        Pattern replace = Pattern.compile(regex);

        boolean isChanged = false;
        for (Repository repository : reps) {

            String id = repository.getId();
            if (isCompliteValue(replace, id)) {
                repository.setId(replace.matcher(id).replaceAll(newName + "$2"));
                isChanged = true;
            }

            String url = repository.getUrl();
            if (isCompliteValue(replace, url)) {
                repository.setUrl(replace.matcher(url).replaceAll(newName + "$2"));
                isChanged = true;
            }
        }
        return isChanged;
    }

    private boolean changeGroupNameInProperties(Properties pr, String oldName, String newName) {
        if (pr == null) {
            return false;
        }
        String regex = "(?i)(" + oldName + ")([\\-\\/])(?<=.)";
        Pattern replace = Pattern.compile(regex);

        boolean isChanged = false;
        for (Entry<Object, Object> property : pr.entrySet()) {
            String value = (String) property.getValue();
            if (isCompliteValue(replace, value)) {
                pr.put(property.getKey(), replace.matcher(value).replaceAll(newName + "$2"));
                isChanged = true;
            }
        }
        return isChanged;
    }

    private boolean changeGroupNameInScm(Scm scm, String oldName, String newName) {
        if (scm == null) {
            return false;
        }
        String regex = "(?i)(" + oldName + ")([\\-\\/])(?<=.)";
        Pattern replace = Pattern.compile(regex);

        boolean isChanged = false;
        String connection = scm.getConnection();
        if (isCompliteValue(replace, connection)) {
            scm.setConnection(replace.matcher(connection).replaceAll(newName + "$2"));
            isChanged = true;
        }

        String devConnection = scm.getDeveloperConnection();
        if (isCompliteValue(replace, devConnection)) {
            scm.setDeveloperConnection(replace.matcher(devConnection).replaceAll(newName + "$2"));
            isChanged = true;
        }
        return isChanged;
    }

    private boolean addRepository(PomXMLModel pomMng, String id, String url) {
        Optional<Model> optModel = pomMng.getModelFile();
        if (!optModel.isPresent()) {
            return false;
        }

        List<Repository> reps = optModel.get().getRepositories();
        Repository repository = new Repository();
        repository.setId(id);
        repository.setUrl(url);
        repository.setLayout(REPOSITORY_LAYOUT);

        if (!isListHasRepository(reps, repository)) {
            reps.add(repository);
            return true;
        }
        return false;
    }

    private boolean modifyRepository(List<Repository> reps, String oldId, String newId, String url) {
        if (reps == null) {
            return false;
        }
        boolean isChanged = false;
        for (Repository repository : reps) {
            if (repository != null && repository.getId().equals(oldId)) {
                repository.setId(newId);
                repository.setUrl(url);
                repository.setLayout(REPOSITORY_LAYOUT);
                isChanged = true;
            }
        }
        return isChanged;
    }

    private boolean removeRepository(PomXMLModel pomMng, String id) {
        Optional<Model> model = pomMng.getModelFile();
        if (!model.isPresent()) {
            return false;
        }
        List<Repository> reps = model.get().getRepositories();
        Repository remRep = null;
        for (Repository repository : reps) {
            if (id.equals(repository.getId())) {
                remRep = repository;
            }
        }
        if (remRep != null) {
            reps.remove(remRep);
            return true;
        }
        return false;
    }

    private boolean isCompliteValue(Pattern replace, String param) {
        if (param == null) {
            return false;
        }
        return replace.matcher(param).find();
    }

    private boolean isListHasRepository(List<Repository> reps, Repository repository) {
        if (reps == null) {
            return false;
        }
        for (Repository rep : reps) {
            if (rep.getId().equals(repository.getId())) {
                return true;
            }
        }
        return false;
    }

    private PomXMLModel getModel(Project project) {
        String pathToPomXML = ""; // TODO: project.getPathToClonedProject()
        return new PomXMLModel(pathToPomXML);
    }

    private boolean isValidString(String value) {
        return value != null && !value.isEmpty();
    }
}
