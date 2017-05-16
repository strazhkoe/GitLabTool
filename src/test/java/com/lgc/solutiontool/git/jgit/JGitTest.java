package com.lgc.solutiontool.git.jgit;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.lgc.solutiontool.git.entities.Group;
import com.lgc.solutiontool.git.entities.Project;
import com.lgc.solutiontool.git.entities.User;
import com.lgc.solutiontool.git.util.JSONParser;

/**
 * Tests for the JGit class.
 *
 * @author Lyudmila Lyska
 */
public class JGitTest {

    private static final String NAME_BRANCH = "test_name";
    private static final String CORRECT_PATH = "/path";

    @Test(expected = IllegalArgumentException.class)
    public void cloneGroupIncorrectDataExceptionGroupTest() {
        JGit.getInstance().clone(null, CORRECT_PATH, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cloneGroupIncorrectDataExceptionPathTest() {
        JGit.getInstance().clone(new Group(), null, null, null);
    }

    @Test
    public void cloneGroupIncorrectDataExceptionTest() {
        Group group = new Group();
        group.setClonedStatus(true);
        Assert.assertFalse(JGit.getInstance().clone(group, ".", null, null));
    }

    @Test
    public void cloneGroupProjectsIsNullTest() {
        Group group = new Group();
        // projects is null, the clone method return false
        Assert.assertFalse(JGit.getInstance().clone(group, CORRECT_PATH, null, null));
    }

    @Test
    public void cloneGroupProjectsIsEmptyTest() {
        Group group = JSONParser.parseToObject("{\"id\":1468081,\"name\":\"STG\", \"projects\":[]}", Group.class);

        // projects is empty, the clone method return false
        Assert.assertFalse(JGit.getInstance().clone(group, CORRECT_PATH, null, null));
        Assert.assertFalse(JGit.getInstance().clone(group, CORRECT_PATH, (progress, project) -> {
        }, (progress, message) -> {
        }));
    }

    @Test
    public void gitcloneRepositoryCorrectDataTest() {
        JGit git = new JGit() {
            @Override
            protected boolean cloneRepository(String linkClone, String localPath)
                    throws InvalidRemoteException, TransportException, GitAPIException {
                return true;
            }
        };

        Group group = JSONParser.parseToObject("{\"id\":1468081,\"name\":\"STG\","
                + "\"projects\":[{\"id\":3010543, \"name\":\"stg\", \"description\":\"\",\"default_branch\":\"master\",\"public\":false,"
                + "\"visibility_level\":0,\"ssh_url_to_repo\":\"git@gitlab.com:SolutionToolGitLab/stg.git\","
                + "\"http_url_to_repo\":\"https://gitlab.com/SolutionToolGitLab/stg.git\","
                + "\"web_url\":\"https://gitlab.com/SolutionToolGitLab/stg\"}]}", Group.class);

        Assert.assertTrue(git.clone(group, CORRECT_PATH, (progress, project) -> {}, (progress, message) -> {}));
    }

    @Test
    public void gitcloneRepositoryIncorrectDataTest() {
        JGit git = new JGit() {
            @Override
            protected boolean cloneRepository(String linkClone, String localPath) throws GitAPIException {
                throw getGitAPIException();
            }
        };

        Group group = JSONParser.parseToObject("{\"id\":1468081,\"name\":\"STG\","
                + "\"projects\":[{\"id\":3010543, \"name\":\"stg\", \"description\":\"\",\"default_branch\":\"master\",\"public\":false,"
                + "\"visibility_level\":0,\"ssh_url_to_repo\":\"git@gitlab.com:SolutionToolGitLab/stg.git\","
                + "\"http_url_to_repo\":\"https://gitlab.com/SolutionToolGitLab/stg.git\","
                + "\"web_url\":\"https://gitlab.com/SolutionToolGitLab/stg\"}]}", Group.class);

        Assert.assertTrue(git.clone(group, CORRECT_PATH, (progress, project) -> {}, (progress, message) -> {}));
    }

    @Test
    public void gitStatusCorrectDataTest() {
        Git gitMock = getGitMock();
        StatusCommand statusCommandMock = new StatusCommand(getRepository()) {
            @Override
            public Status call() throws GitAPIException, NoWorkTreeException {
                return mock(Status.class);
            }
        };
        Mockito.when(gitMock.status()).thenReturn(statusCommandMock);

        Assert.assertTrue(getJGitMock(gitMock).getStatusProject(getProject(true)).isPresent());
    }

    @Test
    public void gitStatusIncorrectDataTest() {
        StatusCommand statusCommandMock = new StatusCommand(getRepository()) {
            @Override
            public Status call() throws GitAPIException, NoWorkTreeException {
                return null;
            }
        };
        Git gitMock = getGitMock();
        Mockito.when(gitMock.status()).thenReturn(statusCommandMock);
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(null).isPresent());
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(false)).isPresent());
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(true)).isPresent());

        statusCommandMock = new StatusCommand(getRepository()) {
            @Override
            public Status call() throws GitAPIException, NoWorkTreeException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.status()).thenReturn(statusCommandMock);
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(true)).isPresent());

        getProject(false).setClonedStatus(true);
        Assert.assertFalse(getJGitMock(gitMock).getStatusProject(getProject(false)).isPresent());
        getProject(false).setClonedStatus(false);
    }

    @Test
    public void addUntrackedFileForCommitCorrectDataTest() {
        Git gitMock = getGitMock();
        AddCommand addCommandMock = new AddCommand(getRepository()) {
            @Override
            public DirCache call() throws GitAPIException {
                return getDirCache();
            }
        };
        Mockito.when(gitMock.add()).thenReturn(addCommandMock);

        List<String> files = new ArrayList<>();
        files.add("0");
        files.add(null);
        Assert.assertTrue(getJGitMock(gitMock).addUntrackedFileForCommit(files, getProject(true)));
    }

    @Test
    public void addUntrackedFileForCommitIncorrectDataTest() {
        Git gitMock = getGitMock();
        AddCommand addCommandMock = new AddCommand(getRepository()) {
            @Override
            public DirCache call() throws GitAPIException, NoFilepatternException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.add()).thenReturn(addCommandMock);
        Assert.assertFalse(getJGitMock(null).addUntrackedFileForCommit(new ArrayList<>(), getProject(false)));
        Assert.assertTrue(getJGitMock(gitMock).addUntrackedFileForCommit(new ArrayList<>(), getProject(true)));

        List<String> files = new ArrayList<>();
        files.add("0");
        files.add(null);
        Assert.assertTrue(getJGitMock(gitMock).addUntrackedFileForCommit(files, getProject(true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUntrackedFileForCommitProjectIsNullTest() {
        JGit.getInstance().addUntrackedFileForCommit(null, getProject(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUntrackedFileForCommitCollectionIsNullTest() {
        JGit.getInstance().addUntrackedFileForCommit(new ArrayList<>(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pullProjectIsNullTest() {
        JGit.getInstance().pull(null);
    }

    @Test
    public void pullIncorrectDataTest() {
        Assert.assertEquals(JGit.getInstance().pull(getProject(false)), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).pull(getProject(true)), JGitStatus.FAILED);

        Git gitMock = getGitMock();
        JGit jGitMock = new JGit() {
            @Override
            protected Optional<Git> getGitForRepository(String path) {
                return Optional.of(gitMock);
            }

            @Override
            protected boolean isContinueMakePull(Project project) {
                return false;
            }
        };
        Assert.assertEquals(jGitMock.pull(getProject(true)), JGitStatus.FAILED);

        PullCommand pullCommandMock = new PullCommand(getRepository()) {
            @Override
            public PullResult call() throws GitAPIException, WrongRepositoryStateException,
                    InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
                    RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.pull()).thenReturn(pullCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).pull(getProject(true)), JGitStatus.FAILED);
    }

    @Test
    public void pullCorrectDataTest() {
        Git gitMock = getGitMock();
        PullResult pullResultMock = mock(PullResult.class);
        PullCommand pullCommandMock = new PullCommand(getRepository()) {
            @Override
            public PullResult call() throws GitAPIException, WrongRepositoryStateException,
                    InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException,
                    RefNotFoundException, RefNotAdvertisedException, NoHeadException, TransportException {
                return pullResultMock;
            }
        };
        Mockito.when(gitMock.pull()).thenReturn(pullCommandMock);

        MergeResult mergeMock = new MergeResult(new ArrayList<>()) {
            @Override
            public MergeStatus getMergeStatus() {
                return MergeStatus.FAST_FORWARD;
            }
        };
        Mockito.when(pullResultMock.getMergeResult()).thenReturn(mergeMock);
        Assert.assertEquals(getJGitMock(gitMock).pull(getProject(true)), JGitStatus.FAST_FORWARD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitMessageIsNullTest() {
        JGit.getInstance().commit(getProjects(), null, false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitMessageIsEmptyTest() {
        JGit.getInstance().commit(getProjects(), "", false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitProjectsIsEmptyTest() {
        JGit.getInstance().commit(new ArrayList<>(), "__", false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitProjectsIsNullTest() {
        JGit.getInstance().commit(null, "__", false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitProjectIsNullTest() {
        JGit.getInstance().commitProject(null, "_", false, null, null, null, null);
    }

    @Test
    public void commitProjectIncorrectDataTest() {
        JGitStatus result = getJGitMock(null).commitProject(getProject(true), "_", false, null, null, null, null);
        Assert.assertEquals(result, JGitStatus.FAILED);

        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);

        result = getJGitMock(gitMock).commitProject(getProject(true), "_", false, null, null, null, null);
        Assert.assertEquals(result, JGitStatus.FAILED);
    }

    @Test
    public void commitProjectCorrectDataTest() {
        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                return mock(RevCommit.class);
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);
        JGitStatus result = getJGitMock(gitMock).commitProject(getProject(true), "_", false, "Lyuda", "l@gmail.com",
                "Lyuda", "l@gmail.com");
        Assert.assertEquals(result, JGitStatus.SUCCESSFUL);
    }

    @Test
    public void commitAllProjectsCorrectDataTest() {
        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                return mock(RevCommit.class);
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);
        JGitStatus result = getJGitMock(gitMock).commit(getProjects(), "_", false, "Lyuda", "l@gmail.com", "Lyuda",
                "l@gmail.com", null, null);
        Assert.assertEquals(result, JGitStatus.SUCCESSFUL);
    }

    @Test
    public void commitAllProjectsIncorrectDataTest() {
        JGitStatus result = getJGitMock(null).commit(getProjects(), "_", false, null, null, null, null, null, null);
        Assert.assertEquals(result, JGitStatus.SUCCESSFUL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pushProjectsIsNullTest() {
        JGit.getInstance().push(null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pushProjectsIsEmptyTest() {
        JGit.getInstance().push(new ArrayList<>(), null, null);
    }

    @Test
    public void pushIncorrectDataTest() {
        Assert.assertTrue(getJGitMock(null).push(getProjects(), null, null));

        Git gitMock = getGitMock();
        PushCommand pushCommandMock = new PushCommand(getRepository()) {
            @Override
            public Iterable<PushResult> call() throws GitAPIException, InvalidRemoteException, TransportException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.push()).thenReturn(pushCommandMock);
        Assert.assertTrue(getJGitMock(gitMock).push(getProjects(), null, null));
    }

    @Test
    public void pushCorrectDataTest() {
        Git gitMock = getGitMock();
        PushCommand pushCommandMock = new PushCommand(getRepository()) {
            @Override
            public Iterable<PushResult> call() throws GitAPIException, InvalidRemoteException, TransportException {
                return Arrays.asList(mock(PushResult.class));
            }
        };
        Mockito.when(gitMock.push()).thenReturn(pushCommandMock);
        Assert.assertTrue(getJGitMock(gitMock).push(getProjects(), (progress) -> {
        }, (progress, message) -> {
        }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushProjectsIsNullTest() {
        JGit.getInstance().commitAndPush(null, "_", false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushMessageIsNullTest() {
        JGit.getInstance().commitAndPush(getProjects(), null, false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushProjectsIsEmptyTest() {
        JGit.getInstance().commitAndPush(new ArrayList<>(), "_", false, null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitAndPushMessageIsEmptyTest() {
        JGit.getInstance().commitAndPush(getProjects(), "", false, null, null, null, null, null, null);
    }

    @Test
    public void commitAndPushIncorrectDataTest() {
        Assert.assertTrue(
                getJGitMock(null).commitAndPush(getProjects(), "__", false, null, null, null, null, null, null));
    }

    @Test
    public void commitAndPushCorrectDataTest() {
        Git gitMock = getGitMock();
        CommitCommand commitCommand = new CommitCommand(getRepository()) {
            @Override
            public RevCommit call() throws GitAPIException, NoHeadException, NoMessageException, UnmergedPathsException,
                    ConcurrentRefUpdateException, WrongRepositoryStateException, AbortedByHookException {
                return mock(RevCommit.class);
            }
        };
        Mockito.when(gitMock.commit()).thenReturn(commitCommand);

        PushCommand pushCommandMock = new PushCommand(getRepository()) {
            @Override
            public Iterable<PushResult> call() throws GitAPIException, InvalidRemoteException, TransportException {
                return Arrays.asList(mock(PushResult.class));
            }
        };
        Mockito.when(gitMock.push()).thenReturn(pushCommandMock);
        Assert.assertTrue(getJGitMock(gitMock).commitAndPush(getProjects(), "__", false, "Lyuda", "l@gmail.com",
                "Lyuda", "l@gmail.com", (progress) -> {
                }, (progress, message) -> {
                }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBranchProjectsIsNullTest() {
        JGit.getInstance().createBranch(null, "__", false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBranchNameBranchIsNullTest() {
        JGit.getInstance().createBranch(new Project(), null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBranchNameBranchIsEmptyTest() {
        JGit.getInstance().createBranch(new Project(), "", false);
    }

    @Test
    public void createBranchIncorrectDataTest() {
        Assert.assertEquals(JGit.getInstance().createBranch(new Project(), "__", false), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).createBranch(getProject(true), "__", false), JGitStatus.FAILED);

        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = new ListBranchCommand(getRepository()) {
            @Override
            public List<Ref> call() throws GitAPIException {
                throw getGitAPIException();
            }
        };
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        Ref refMock = mock(Ref.class);
        listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_REMOTES + NAME_BRANCH);
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, false), JGitStatus.BRANCH_ALREADY_EXISTS);

        CreateBranchCommand createBranchCommandMock = new CreateBranchCommand(getRepository()) {
            @Override
            public Ref call()
                    throws GitAPIException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException {
                throw getGitAPIException();
            }
        };
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchCreate()).thenReturn(createBranchCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);
    }

    @Test
    public void createBranchCorrectDataTest() {
        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);
        CreateBranchCommand createBranchCommandMock = new CreateBranchCommand(getRepository()) {
            @Override
            public Ref call()
                    throws GitAPIException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException {
                return refMock;
            }
        };
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchCreate()).thenReturn(createBranchCommandMock);
        Assert.assertEquals(getJGitMock(gitMock).createBranch(getProject(true), NAME_BRANCH, true),
                JGitStatus.SUCCESSFUL);
    }

    @Test(expected=IllegalArgumentException.class)
    public void getBranchesProjectNullTest() {
        JGit.getInstance().getBranches(null, BranchType.LOCAL);
    }

    @Test(expected=IllegalArgumentException.class)
    public void getBranchesBranchTypeIsNullTest() {
        JGit.getInstance().getBranches(new Project(), null);
    }

    @Test
    public void getBranchesCorrectData() {
        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProject(true), BranchType.LOCAL).isEmpty());
        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProject(true), BranchType.REMOTE).isEmpty());
    }

    @Test
    public void getBranchesIncorrectData() {
        Git gitMock = getGitMock();
        Assert.assertTrue(getJGitMock(null).getBranches(getProject(true), BranchType.REMOTE).isEmpty());
        Assert.assertTrue(getJGitMock(gitMock).getBranches(getProject(false), BranchType.REMOTE).isEmpty());

    }

    @Test(expected=IllegalArgumentException.class)
    public void getBranchesWithParametersProjectNullTest() {
        JGit.getInstance().getBranches(null, BranchType.LOCAL, false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void getBranchesWithParametersBranchTypeIsNullTest() {
        JGit.getInstance().getBranches(new ArrayList<>(), null, false);
    }

    @Test
    public void getBranchesWithParametersCorrectData() {
        Assert.assertTrue(JGit.getInstance().getBranches(new ArrayList<>(), BranchType.LOCAL, false).isEmpty());
        Assert.assertTrue(JGit.getInstance().getBranches(new ArrayList<>(), BranchType.REMOTE, false).isEmpty());

        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProjects(), BranchType.LOCAL, true).isEmpty());
        Assert.assertFalse(getJGitMock(gitMock).getBranches(getProjects(), BranchType.ALL, false).isEmpty());
    }

    @Test(expected=IllegalArgumentException.class)
    public void getCurrentBranchProjectIsNullTest() {
        JGit.getInstance().getCurrentBranch(null);
    }

    @Test
    public void getCurrentBranchIncorrectData() {
        Assert.assertFalse(JGit.getInstance().getCurrentBranch(getProject(false)).isPresent());
        Assert.assertFalse(getJGitMock(null).getCurrentBranch(getProject(true)).isPresent());

        Git gitMock = getGitMock();
        Repository repoMock = getRepo(null);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertFalse(getJGitMock(gitMock).getCurrentBranch(getProject(true)).isPresent());
    }

    @Test
    public void getCurrentBranchCorrectData() {
        Git gitMock = getGitMock();
        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertTrue(getJGitMock(gitMock).getCurrentBranch(getProject(true)).isPresent());
    }

    @Test(expected=IllegalArgumentException.class)
    public void deleteBranchProjectIsNullTest() {
        JGit.getInstance().deleteBranch(null, "", false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void deleteBranchNameBranchIsNullTest() {
        JGit.getInstance().deleteBranch(getProject(false), null, false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void deleteBranchNameBranchIsEmptyTest() {
        JGit.getInstance().deleteBranch(getProject(false), "", false);
    }

    @Test
    public void deleteBranchIncorrectDataTest() {
        Assert.assertEquals(JGit.getInstance().deleteBranch(getProject(false), NAME_BRANCH, false), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        Git gitMock = getGitMock();
        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertEquals(getJGitMock(gitMock).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        DeleteBranchCommand deleteBranchMock = new DeleteBranchCommand(getRepository()) {
            @Override
            public DeleteBranchCommand setBranchNames(String... branchnames) {
                return super.setBranchNames(NAME_BRANCH);
            }
            @Override
            public List<String> call() throws GitAPIException, NotMergedException, CannotDeleteCurrentBranchException {
                throw getGitAPIException();
            }
        };
        repoMock = getRepo(null);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Mockito.when(gitMock.branchDelete()).thenReturn(deleteBranchMock);
        Assert.assertEquals(getJGitMock(gitMock).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);
    }

    @Test
    public void deleteBranchCorrectDataTest() {
        Git gitMock = getGitMock();
        Repository repoMock = getRepo(null);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        DeleteBranchCommand deleteBranchMock = new DeleteBranchCommand(getRepository()) {
            @Override
            public DeleteBranchCommand setBranchNames(String... branchnames) {
                return super.setBranchNames(NAME_BRANCH);
            }
            @Override
            public List<String> call() throws GitAPIException, NotMergedException, CannotDeleteCurrentBranchException {
                return Collections.emptyList();
            }
        };
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Mockito.when(gitMock.branchDelete()).thenReturn(deleteBranchMock);
        Assert.assertEquals(getJGitMock(gitMock).deleteBranch(getProject(true), NAME_BRANCH, false), JGitStatus.SUCCESSFUL);
    }

    @Test(expected=IllegalArgumentException.class)
    public void switchToProjectIsNullTest() {
        JGit.getInstance().switchTo(null, "__", false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void switchToBranchNameIsNullTest() {
        JGit.getInstance().switchTo(getProject(false), null, true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void switchToBranchNameIsEmptyTest() {
        JGit.getInstance().switchTo(getProject(true), "", false);
    }

    @Test
    public void switchToIncorrectDataTest() {
        Assert.assertEquals(getJGitMock(null).switchTo(getProject(false), NAME_BRANCH, false), JGitStatus.FAILED);
        Assert.assertEquals(getJGitMock(null).switchTo(getProject(true), NAME_BRANCH, false), JGitStatus.FAILED);

        Ref refMock = mock(Ref.class);
        Git gitMock = getGitMock();
        ListBranchCommand listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.toString()).thenReturn(Constants.R_HEADS);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + "Test");
        Mockito.when(gitMock.branchList()).thenReturn(listCommandMock);

        Repository repoMock = getRepo(NAME_BRANCH);
        Mockito.when(gitMock.getRepository()).thenReturn(repoMock);
        Assert.assertEquals(getJGitMock(gitMock).switchTo(getProject(true), NAME_BRANCH, false), JGitStatus.BRANCH_DOES_NOT_EXIST);
        Assert.assertEquals(getJGitMock(gitMock).switchTo(getProject(true), NAME_BRANCH, true), JGitStatus.FAILED);

        listCommandMock = getListCommandMock(refMock);
        Mockito.when(refMock.getName()).thenReturn(Constants.R_HEADS + NAME_BRANCH);
        Assert.assertEquals(getJGitMock(gitMock).switchTo(getProject(true), NAME_BRANCH, true), JGitStatus.BRANCH_ALREADY_EXISTS);

    }


    /*************************************************************************************************/
    private Project getProject(boolean isCorrectProject) {
        if (!isCorrectProject) {
            return new Project();
        }
        Project projectCorrect = new Project() {
            @Override
            protected boolean checkPath(Path pathToProject) {
                return true;
            };
        };
        projectCorrect.setPathToClonedProject(".path");
        projectCorrect.setClonedStatus(true);
        return projectCorrect;
    }

    private List<Project> getProjects() {
        List<Project> listProjects = new ArrayList<>();
        listProjects.add(getProject(true));
        listProjects.add(null);
        listProjects.add(getProject(false));
        listProjects.add(getProject(true));
        return listProjects;
    }

    private JGit getJGitMock(Git gitMock) {
        if (gitMock == null) {
            return new JGit() {
                @Override
                protected Optional<Git> getGitForRepository(String path) {
                    return Optional.empty();
                }
            };
        }

        JGit correctJGitMock = new JGit() {
            @Override
            protected Optional<Git> getGitForRepository(String path) {
                return Optional.of(gitMock);
            }

            @Override
            protected boolean isContinueMakePull(Project project) {
                return true;
            }

            @Override
            protected User getUserData() {
                User user = new User("Lyudmila", "ld@email.com");
                return user;
            }
        };
        return correctJGitMock;
    }

    private Git getGitMock() {
        return mock(Git.class);
    }

    private Repository getRepository() {
        return mock(Repository.class);
    }

    private Repository getRepo(String nameBranch) {
        BaseRepositoryBuilder<?, ?> buildMock = mock(BaseRepositoryBuilder.class);
        if (nameBranch == null) {
            return new Repository(buildMock) {

                @Override
                public String getFullBranch() throws IOException {
                    throw new IOException();
                }

                @Override
                public String getBranch() throws IOException {
                    throw new IOException();
                }

                @Override
                public void scanForRepoChanges() throws IOException {
                }

                @Override
                public void notifyIndexChanged() {
                }

                @Override
                public ReflogReader getReflogReader(String refName) throws IOException {
                    return null;
                }

                @Override
                public RefDatabase getRefDatabase() {
                    return null;
                }

                @Override
                public ObjectDatabase getObjectDatabase() {
                    return null;
                }

                @Override
                public StoredConfig getConfig() {
                    return null;
                }

                @Override
                public AttributesNodeProvider createAttributesNodeProvider() {
                    return null;
                }

                @Override
                public void create(boolean bare) throws IOException {

                }
            };
        }
        Repository repoMock = new Repository(buildMock) {

            @Override
            public String getFullBranch() throws IOException {
                return Constants.R_HEADS + nameBranch;
            }

            @Override
            public String getBranch() throws IOException {
                return nameBranch;
            }

            @Override
            public void scanForRepoChanges() throws IOException {
            }

            @Override
            public void notifyIndexChanged() {
            }

            @Override
            public ReflogReader getReflogReader(String refName) throws IOException {
                return null;
            }

            @Override
            public RefDatabase getRefDatabase() {
                return null;
            }

            @Override
            public ObjectDatabase getObjectDatabase() {
                return null;
            }

            @Override
            public StoredConfig getConfig() {
                return null;
            }

            @Override
            public AttributesNodeProvider createAttributesNodeProvider() {
                return null;
            }

            @Override
            public void create(boolean bare) throws IOException {

            }
        };
        return repoMock;
    }


    private ListBranchCommand getListCommandMock(Ref ref) {
        ListBranchCommand listCommandMock = new ListBranchCommand(getRepository()) {
            @Override
            public List<Ref> call() throws GitAPIException {
                List<Ref> refs = new ArrayList<>();
                refs.add(ref);
                return refs;
            }
        };
        return listCommandMock;
    }

    private GitAPIException getGitAPIException() {
        return mock(GitAPIException.class);
    }

    private DirCache getDirCache() {
        return mock(DirCache.class);
    }

}
