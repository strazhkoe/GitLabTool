package com.lgc.gitlabtool.git.ui;

/**
 * Enum-helper for managing viewWindow names
 *
 * @author Pavlo Pidhorniy
 */
public enum ViewKey {

    //GROUPS_WINDOW and PROJECTS_WINDOW used only for separating two parts of Modular controller
    GROUPS_WINDOW("groupWindow", null),
    PROJECTS_WINDOW("mainWindow", null),
    ALL_WINDOWS("allWindows", null),
    COMMON_VIEW("commonView", null),

    MODULAR_CONTAINER("modularContainer", "fxml/ModularContainer.fxml"),
    CLONING_GROUPS_WINDOW("cloningGroupsWindow", "fxml/CloningGroupsWindow.fxml"),
    BRANCHES_WINDOW("branchesWindow", "fxml/BranchesWindow.fxml"),
    SERVER_INPUT_WINDOW("serverInputWindow", "fxml/ServerInputWindow.fxml"),
    EDIT_PROJECT_PROPERTIES("editProjectProperties", "fxml/EditProjectPropertiesWindow.fxml"),
    GIT_STAGING_WINDOW("gitStagingWindow", "fxml/GitStagingWindow.fxml"),
    STASH_WINDOW("stashWindow", "fxml/StashWindow.fxml");

    private final String key;
    private final String path;

    /**
     * Returns id of viewWindow (using for some ui operations: toolbar, etc)
     *
     * @return id of viewWindow
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns path of viewWindow (using for some ui operations: toolbar, etc)
     *
     * @return path of viewWindow
     */
    public String getPath() {
        return path;
    }


    private ViewKey(final String selectedKey, final String selectedPath) {
        this.key = selectedKey;
        this.path = selectedPath;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return key + " (" + path + ")";
    }
}
