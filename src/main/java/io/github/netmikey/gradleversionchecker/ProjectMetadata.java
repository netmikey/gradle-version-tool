package io.github.netmikey.gradleversionchecker;

import java.io.File;

/**
 * Holds the metadata collected about a given project directory.
 */
public class ProjectMetadata {

    private File projectDir;

    private boolean underGitVersionControl;

    private boolean gradleVersionUpToDate;

    private boolean gradleVersionSameMajorAsCurrent;

    private String gradleVersion;

    private String branch;

    private boolean clean;

    private boolean gitRemoteTracking;

    private int gitAheadCount;

    private int gitBehindCount;

    /**
     * Indicates whether the project's local Git repository is in sync with the
     * remote repository.
     * 
     * @return <code>true</code> if in sync or no remote tracking set up.
     */
    public boolean isInSyncWithRemote() {
        return !gitRemoteTracking || (gitAheadCount == 0 && gitBehindCount == 0);
    }

    /**
     * Get the projectDir.
     * 
     * @return Returns the projectDir.
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * Set the projectDir.
     * 
     * @param projectDir
     *            The projectDir to set.
     */
    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * Get the underGitVersionControl.
     * 
     * @return Returns the underGitVersionControl.
     */
    public boolean isUnderGitVersionControl() {
        return underGitVersionControl;
    }

    /**
     * Set the underGitVersionControl.
     * 
     * @param underGitVersionControl
     *            The underGitVersionControl to set.
     */
    public void setUnderGitVersionControl(boolean underGitVersionControl) {
        this.underGitVersionControl = underGitVersionControl;
    }

    /**
     * Get the gradleVersionUpToDate.
     * 
     * @return Returns the gradleVersionUpToDate.
     */
    public boolean isGradleVersionUpToDate() {
        return gradleVersionUpToDate;
    }

    /**
     * Set the gradleVersionUpToDate.
     * 
     * @param gradleVersionUpToDate
     *            The gradleVersionUpToDate to set.
     */
    public void setGradleVersionUpToDate(boolean gradleVersionUpToDate) {
        this.gradleVersionUpToDate = gradleVersionUpToDate;
    }

    /**
     * Get the gradleVersion.
     * 
     * @return Returns the gradleVersion.
     */
    public String getGradleVersion() {
        return gradleVersion;
    }

    /**
     * Set the gradleVersion.
     * 
     * @param gradleVersion
     *            The gradleVersion to set.
     */
    public void setGradleVersion(String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }

    /**
     * Get the branch.
     * 
     * @return Returns the branch.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Set the branch.
     * 
     * @param branch
     *            The branch to set.
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * Get the clean.
     * 
     * @return Returns the clean.
     */
    public boolean isClean() {
        return clean;
    }

    /**
     * Set the clean.
     * 
     * @param clean
     *            The clean to set.
     */
    public void setClean(boolean clean) {
        this.clean = clean;
    }

    /**
     * Get the gradleVersionSameMajorAsCurrent.
     * 
     * @return Returns the gradleVersionSameMajorAsCurrent.
     */
    public boolean isGradleVersionSameMajorAsCurrent() {
        return gradleVersionSameMajorAsCurrent;
    }

    /**
     * Set the gradleVersionSameMajorAsCurrent.
     * 
     * @param gradleVersionSameMajorAsCurrent
     *            The gradleVersionSameMajorAsCurrent to set.
     */
    public void setGradleVersionSameMajorAsCurrent(boolean gradleVersionSameMajorAsCurrent) {
        this.gradleVersionSameMajorAsCurrent = gradleVersionSameMajorAsCurrent;
    }

    /**
     * Get the gitRemoteTracking.
     * 
     * @return Returns the gitRemoteTracking.
     */
    public boolean isGitRemoteTracking() {
        return gitRemoteTracking;
    }

    /**
     * Set the gitRemoteTracking.
     * 
     * @param gitRemoteTracking
     *            The gitRemoteTracking to set.
     */
    public void setGitRemoteTracking(boolean gitRemoteTracking) {
        this.gitRemoteTracking = gitRemoteTracking;
    }

    /**
     * Get the gitAheadCount.
     * 
     * @return Returns the gitAheadCount.
     */
    public int getGitAheadCount() {
        return gitAheadCount;
    }

    /**
     * Set the gitAheadCount.
     * 
     * @param gitAheadCount
     *            The gitAheadCount to set.
     */
    public void setGitAheadCount(int gitAheadCount) {
        this.gitAheadCount = gitAheadCount;
    }

    /**
     * Get the gitBehindCount.
     * 
     * @return Returns the gitBehindCount.
     */
    public int getGitBehindCount() {
        return gitBehindCount;
    }

    /**
     * Set the gitBehindCount.
     * 
     * @param gitBehindCount
     *            The gitBehindCount to set.
     */
    public void setGitBehindCount(int gitBehindCount) {
        this.gitBehindCount = gitBehindCount;
    }

}
