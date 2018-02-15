package com.lgc.gitlabtool.git.ui.javafx.controllers;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.lgc.gitlabtool.git.services.*;
import org.apache.commons.lang.StringUtils;

import com.lgc.gitlabtool.git.entities.Branch;
import com.lgc.gitlabtool.git.entities.MessageType;
import com.lgc.gitlabtool.git.entities.Project;
import com.lgc.gitlabtool.git.entities.ProjectList;
import com.lgc.gitlabtool.git.jgit.BranchType;
import com.lgc.gitlabtool.git.listeners.stateListeners.AbstractStateListener;
import com.lgc.gitlabtool.git.listeners.stateListeners.ApplicationState;
import com.lgc.gitlabtool.git.ui.icon.LocalRemoteIconHolder;
import com.lgc.gitlabtool.git.ui.javafx.ChangesCheckDialog;
import com.lgc.gitlabtool.git.ui.javafx.CheckoutBranchProgressDialog;
import com.lgc.gitlabtool.git.ui.javafx.ProgressDialog;
import com.lgc.gitlabtool.git.ui.javafx.listeners.OperationProgressListener;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("unchecked")
public class CheckoutBranchWindowController extends AbstractStateListener {

    private static final String TOTAL_CAPTION = "Total count: ";

    private List<Branch> _allBranches = new ArrayList<>();

    private List<Integer> _selectedProjectsIds = new ArrayList<>();
    private ProjectList _projectList;

    private Stage _stage;

    @FXML
    private ListView<Project> currentProjectsListView;

    @FXML
    private Label projectsCountLabel;

    @FXML
    private ToggleGroup branchesFilter;

    @FXML
    private ListView<Branch> branchesListView;

    @FXML
    private CheckBox commonMatchingCheckBox;

    @FXML
    private TextField searchField;

    @FXML
    private Label branchesCountLabel;

    @FXML
    private Button checkoutButton;

    private static final StateService _stateService = ServiceProvider.getInstance().getService(StateService.class);

    private static final ConsoleService _consoleService = ServiceProvider.getInstance().getService(ConsoleService.class);

    private static final BackgroundService _backgroundService = ServiceProvider.getInstance()
            .getService(BackgroundService.class);

    private static final GitService _gitService = ServiceProvider.getInstance().getService(GitService.class);

    private static final ThemeService _themeService = (ThemeService) ServiceProvider.getInstance()
            .getService(ThemeService.class);

    private static final String ALREADY_CHECKED_OUT_MESSAGE = "%d of %d projects have already checked out the selected branch.";


    {
        _stateService.addStateListener(ApplicationState.LOAD_PROJECTS, this);
        _stateService.addStateListener(ApplicationState.UPDATE_PROJECT_STATUSES, this);
    }

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterPlantList(oldValue, newValue));
        disableSwitchButton(null);
    }

    void beforeShowing(List<Project> projects, Stage stage) {
        _stage = stage;
        _stage.addEventFilter(WindowEvent.WINDOW_HIDDEN, event -> dispose());

        _projectList = ProjectList.get(null);
        _selectedProjectsIds = ProjectList.getIdsProjects(projects);
        setProjectListItems(getProjectsByIds(), currentProjectsListView);

        configureProjectsListView(currentProjectsListView);
        configureBranchesListView(branchesListView);

        onUpdateList();
    }

    /*
    Buttons
    */
    public void onCheckoutButton() {
        List<Project> selectedProjects = currentProjectsListView.getItems();
        Branch selectedBranch = branchesListView.getSelectionModel().getSelectedItem();

        List<Project> correctProjects = selectedProjects.stream()
                                                      .filter(project -> !Branch.compareBranches(project, selectedBranch))
                                                      .collect(Collectors.toList());
        int numberSelected = selectedProjects.size();
        if (correctProjects.size() != numberSelected) {
            _consoleService.addMessage(
                    String.format(ALREADY_CHECKED_OUT_MESSAGE, numberSelected - correctProjects.size(), numberSelected),
                    MessageType.ERROR);
        }

        List<Project> changedProjects = _gitService.getProjectsWithChanges(correctProjects);
        if (changedProjects.isEmpty()) {
            checkoutBranch(correctProjects, selectedBranch);
        } else {
            launchCheckoutBranchConfirmation(changedProjects, correctProjects, selectedBranch);
        }
    }

    private List<Project> getProjectsByIds() {
        return _projectList.getProjectsByIds(_selectedProjectsIds);
    }

    private void checkoutBranch(List<Project> selectedProjects, Object selectedBranch) {
        ProgressDialog progressDialog = new CheckoutBranchProgressDialog();
        progressDialog.setStartAction(() -> checkoutBranch(selectedBranch, selectedProjects, progressDialog));
        progressDialog.showDialog();
    }

    private void checkoutBranch(Object selectedBranch, List<Project> projects, ProgressDialog progressDialog) {
        OperationProgressListener switchProgressListener =
                new OperationProgressListener(progressDialog, ApplicationState.CHECKOUT_BRANCH);
        _gitService.checkoutBranch(projects, (Branch) selectedBranch, switchProgressListener);
    }

    private void launchCheckoutBranchConfirmation(List<Project> changedProjects,
                                                List<Project> selectedProjects, Branch selectedBranch) {

        ChangesCheckDialog alert = new ChangesCheckDialog();
        alert.launchConfirmationDialog(changedProjects, selectedProjects, selectedBranch, this::checkoutBranch);
    }

    public void onClose() {
        _stage.close();
    }

    public void onUpdateList() {
        RadioButton selecteRB = (RadioButton) branchesFilter.getSelectedToggle();
        String branchTypeText = selecteRB.getText();

        Boolean isCommonMatching = commonMatchingCheckBox.isSelected();

        BranchType branchType;
        switch (branchTypeText) {
            case "Remote":
                branchType = BranchType.REMOTE;
                break;
            case "Local":
                branchType = BranchType.LOCAL;
                break;
            case "Remote + Local":
                branchType = BranchType.ALL;
                break;
            default:
                branchType = BranchType.LOCAL;
        }

        _allBranches = getBranches(getProjectsByIds(), branchType, isCommonMatching);
        branchesListView.getSelectionModel().clearSelection();
        branchesListView.setItems(FXCollections.observableArrayList(_allBranches));

        searchField.setText(StringUtils.EMPTY);
        currentProjectsListView.setItems(FXCollections.observableArrayList(getProjectsByIds()));
    }

    private void filterPlantList(String oldValue, String newValue) {

        List<Branch> filteredBranchList = new ArrayList<>();

        if (searchField == null || searchField.getText().equals(StringUtils.EMPTY)) {
            branchesListView.setItems(FXCollections.observableArrayList(_allBranches));
            currentProjectsListView.setItems(FXCollections.observableArrayList(getProjectsByIds()));
        } else {
            //filtering branches
            newValue = newValue.toUpperCase();
            for (Object branch : _allBranches) {
                String filterText = ((Branch) branch).getBranchName();
                if (filterText.toUpperCase().contains(newValue)) {
                    filteredBranchList.add((Branch) branch);
                }
            }
            branchesListView.getItems().clear();
            branchesListView.setItems(FXCollections.observableArrayList(filteredBranchList));

            filteringProjectsListView(filteredBranchList);

        }
    }

    private void filteringProjectsListView(List<Branch> branches) {
        List<Project> filteredProjectList = new ArrayList<>();

        //filtering projects
        for (Object project : getProjectsByIds()) {
            if (_gitService.containsBranches((Project) project, branches, false)) {
                filteredProjectList.add((Project) project);
            }
        }
        currentProjectsListView.getItems().clear();
        currentProjectsListView.setItems(FXCollections.observableArrayList(filteredProjectList));
    }

    private List<Branch> getBranches(List<Project> selectedProjects, BranchType branchType, Boolean isCommonMatching) {
        Set<Branch> allBranchesWithTypes = _gitService.getBranches(selectedProjects, branchType, isCommonMatching);
        List<Branch> list = new ArrayList<>(allBranchesWithTypes);
        Collections.sort(list, (o1, o2) -> {

            String type1 = o1.getBranchType().name();
            String type2 = o2.getBranchType().name();
            int sComp = type1.compareTo(type2);

            if (sComp != 0) {
                return sComp;
            } else {
                String name1 = o1.getBranchName();
                String name2 = o2.getBranchName();
                return name1.compareTo(name2);
            }
        });

        return list;
    }

    private void setProjectListItems(List items, ListView<Project> listView) {
        if (items == null || items.isEmpty()) {
            return;
        }

        if (items.get(0) instanceof Project) {
            listView.setItems(FXCollections.observableArrayList(items));
        }
    }

    private void configureProjectsListView(ListView listView) {
        //config displayable string
        listView.setCellFactory(p -> new ProjectListCell());

        listView.itemsProperty().addListener((observable, oldValue, newValue) ->
                projectsCountLabel.textProperty().bind(Bindings.concat(TOTAL_CAPTION,
                        Bindings.size((listView.getItems())).asString())));

        //disabling selection
        listView.getSelectionModel().selectedIndexProperty().addListener(
                (observable, oldvalue, newValue) -> Platform.runLater(() -> listView.getSelectionModel().select(-1)));
    }

    private void configureBranchesListView(ListView listView) {
        //config displayable string with icon
        listView.setCellFactory(p -> new BranchListCell());


        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ArrayList<Branch> selectedValue = new ArrayList<>();
                selectedValue.add((Branch) newValue);
                filteringProjectsListView(selectedValue);
            }
        });

        listView.itemsProperty().addListener((observable, oldValue, newValue) ->
                branchesCountLabel.textProperty().bind(Bindings.concat(TOTAL_CAPTION,
                        Bindings.size((listView.getItems())).asString())));

        branchesListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Branch branch = newValue;
                filteringProjectsListView(Arrays.asList(branch));

                disableSwitchButton(branch);
            }
        });
    }

    private void disableSwitchButton(Object currentBranch) {
        if (currentBranch == null) {
            BooleanBinding defaultProperty = branchesListView.getSelectionModel().selectedItemProperty().isNull();
            checkoutButton.disableProperty().bind(defaultProperty);
            return;
        }
        if(isSelectedBranchCurrentForAllProjects((Branch)currentBranch)) {
            checkoutButton.disableProperty().bind(new SimpleBooleanProperty(true));
        } else {
            checkoutButton.disableProperty().bind(new SimpleBooleanProperty(false));
        }
    }

    private boolean isSelectedBranchCurrentForAllProjects(Branch currentBranch) {
        return !currentProjectsListView.getItems().parallelStream()
                                                  .filter(project -> !Branch.compareBranches(project, currentBranch))
                                                  .findFirst()
                                                  .isPresent();
    }

    private class BranchListCell extends ListCell<Branch> {

        @Override
        protected void updateItem(Branch item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);

            if (item != null && !empty) {
                Image fxImage = getBranchIcon(item);
                ImageView imageView = new ImageView(fxImage);

                setGraphic(imageView);
                setText(item.getBranchName());
            }
        }

        private Image getBranchIcon(Branch item) {

            BranchType type = item.getBranchType();
            Image branchIcon;
            if (type == BranchType.LOCAL) {
                branchIcon = LocalRemoteIconHolder.getInstance().getLocalBranchIcoImage();
            } else {
                branchIcon = LocalRemoteIconHolder.getInstance().getRemoteBranchIcoImage();
            }
            return branchIcon;
        }
    }

    @Override
    public void handleEvent(ApplicationState changedState, boolean isActivate) {
        if (!isActivate) {
            _backgroundService.runInBackgroundThread(() -> {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        String textSearch = searchField.getText();
                        Branch branch = branchesListView.getSelectionModel().getSelectedItem();
                        if (textSearch != null && !textSearch.isEmpty() && branch == null) {
                            filterPlantList(null, textSearch);
                        } else if (branch != null) {
                            filteringProjectsListView(Arrays.asList(branch));
                        } else {
                            currentProjectsListView.setItems(FXCollections.observableArrayList(getProjectsByIds()));
                        }
                        disableSwitchButton(branch);
                    }
                });
            });
        }
    }

}