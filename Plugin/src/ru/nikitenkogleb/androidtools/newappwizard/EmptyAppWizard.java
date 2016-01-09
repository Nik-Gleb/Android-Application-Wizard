package ru.nikitenkogleb.androidtools.newappwizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.core.runtime.*;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.*;

import java.io.*;

import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;


import ru.nikitenkogleb.androidtools.newappwizard.WizardPage.WizardCallback;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "mpe". If a sample multi-page editor (also available
 * as a template) is registered for the same extension it will
 * be able to open it.
 */

@SuppressWarnings("restriction")
public class EmptyAppWizard extends Wizard implements INewWizard,
								IExecutableExtension, WizardCallback {
	

	/**	Standard Android build commands for buildSpec */
	private static final String[] BUILD_COMMANDS = new String[] {
			"com.android.ide.eclipse.adt.ResourceManagerBuilder",
			"com.android.ide.eclipse.adt.PreCompilerBuilder",
			"org.eclipse.jdt.core.javabuilder",
			"com.android.ide.eclipse.adt.ApkBuilder"
	};
	/**	Standard Android natures */
	private static final String[] NATURES = new String[] {
			"com.android.ide.eclipse.adt.AndroidNature",
			"org.eclipse.jdt.core.javanature"
	};
	
	/**	All folders in project. */
	private static final String[] FOLDERS = {
			".settings", "src", "res", "bin", "gen",
			"res/mipmap-ldpi", "res/mipmap-mdpi", "res/mipmap-hdpi",
			"res/mipmap-xhdpi", "res/mipmap-xxhdpi", "res/mipmap-xxxhdpi",
			"res/layout", "res/values"
	};
	
	/** All sources folders in project. */
	private static final String[] SRC_FOLDERS = {"app", "utils"};

	
	/**	The name of plugin console for outputs. */
	private static final String PLUGIN_CONSOLE_NAME = "System Output";
	
	/**	String prefix for temp project directory. */
	private static final String TEMP_PREFFIX = "_temp";
	
	@SuppressWarnings("unused")
	private static final Locale LOCALE = Locale.getDefault();
		
	/**	Wizard Page */
	private final WizardPage mWizardPage = new WizardPage(this);

	private IConfigurationElement config;

	private IWorkbench workbench;

	@SuppressWarnings("unused")
	private IStructuredSelection selection;

	private IProject project;
	
	/**	Debug console */
	private MessageConsoleStream mMessageConsoleStream;
	
	/**	Project name key */
	private static final String KEY_PROJECT_NAME = "projectName";
	/**	Package name key */
	private static final String KEY_PACKAGE_NAME = "packageName";
	/**	Package path key */
	private static final String KEY_PACKAGE_PATH = "packagePath";
	/**	Target api key */
	private static final String KEY_TARGET_API = "targetApi";
	/**	Target api level key */
	private static final String KEY_API_LEVEL = "levelApi";
	/**	SDK path key */
	private static final String KEY_SDK_PATH = "sdkPath";
	/**	LICENSE key */
	private static final String KEY_LICENSE = "license";
	/**	Author key */
	private static final String KEY_AUTHOR = "author";
	/**	Date key */
	private static final String KEY_DATE = "date";

	/** Constructor for EmptyAppWizard. */
	public EmptyAppWizard() {
		super();
		final MessageConsole messageConsole = findConsole(PLUGIN_CONSOLE_NAME);
		if (messageConsole != null)
			mMessageConsoleStream =	messageConsole.newMessageStream();
	}
	
	/** Adding the page to the wizard. */
	public void addPages() {addPage(mWizardPage);}
		
	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		
		if (project != null) return true;

		final IProject projectHandle = mWizardPage.getProjectHandle();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		
		
		final String projectName = projectHandle.getName();
		final String tempProject = projectName + TEMP_PREFFIX;
		final String projectNameLowerCase = projectName.toLowerCase(Locale.getDefault());
		final String packageName = mWizardPage.getPackageName() + "." +	projectNameLowerCase;
		final String targetApi = mWizardPage.getTargetApi();
		final String gitRepository = mWizardPage.getGitRepository();
		final String login = mWizardPage.getGitUserName();
		final String password = mWizardPage.getGitUserPassword();
		final String user = mWizardPage.getGitAuthorName();
		final String email = mWizardPage.getGitAuthorEmail();
		final String gitBranch = mWizardPage.getGitBranch();
		final String commitMessage = mWizardPage.getGitCommitMessage();
		
		final IProjectDescription description = workspace
				.newProjectDescription(projectHandle.getName());
		
		final URI projectURI = (!mWizardPage.useDefaults()) ?
				mWizardPage.getLocationURI() : null;
				
		description.setLocationURI(projectURI);
		description.setNatureIds(NATURES);
		final ICommand[] commands = new ICommand[BUILD_COMMANDS.length];
		for (int i = 0; i < commands.length; i++) {
			final ICommand command = new BuildCommand();
			command.setBuilderName(BUILD_COMMANDS[i]);
			commands[i] = command;
		}
		
		description.setBuildSpec(commands);
		
		final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor)
					throws CoreException {
				createProject(description, projectHandle, monitor,
						projectName, projectNameLowerCase, packageName,
						targetApi, tempProject,
						gitRepository, login, password, user, email,
						gitBranch, commitMessage);
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {return false;}
		catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}

		project = projectHandle;

		if (project == null) return false;
		
		BasicNewProjectResourceWizard.updatePerspective(config);
		BasicNewProjectResourceWizard.selectAndReveal(project,
				workbench.getActiveWorkbenchWindow());

		return true;
	}

	
	/** This creates the project in the workspace. */
	/**
	 * Create new android project with all necessary keys.
	 * 
	 * @param description	project description
	 * @param proj			project instance
	 * @param monitor		monitor for asynchronous
	 * @param projectName	project name
	 * @param projectNameLC	project name in lower case
	 * @param packageName	the name of project package
	 * @param targetApi		target android api
	 * @param tempProject	temp folder path
	 * @param gitRepository	path to git-repository
	 * 
	 * @throws CoreException	causes if anything was failed
	 * @throws OperationCanceledException causes after user-cancel action
	 */
	@SuppressWarnings("deprecation")
	void createProject(IProjectDescription description, IProject proj, IProgressMonitor monitor,
			String projectName,	String projectNameLC, String packageName,
			String targetApi, String tempProject,
			String gitRepository,
			String gitLogin, String gitPassword,
			String gitName, String gitEmail,
			String gitBranch, String gitMessage) throws CoreException,
			OperationCanceledException {
		try {
			monitor.beginTask("", 2000);
			
			
			proj.create(description, new SubProgressMonitor(monitor, 1000));
			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));
			
			/*try {proj.setDefaultCharset("UTF-8", monitor);}
			catch (CoreException e) {e.printStackTrace();}*/
			
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			/*
			 * Okay, now we have the project and we can do more things with it
			 * before updating the perspective.
			 */
			final IContainer container = (IContainer) proj;

			createFolders(packageName, container, monitor);
			
			final String pathPackage = packageName.replace(".", "/");

			String license = "";
			
			GitSupport gitSupport = new GitSupport(gitLogin, gitPassword,
					gitRepository, proj.getLocation().toString(), tempProject, gitBranch);
			
			if (gitSupport.isSuccessful()) {
				description = proj.getDescription();
				description.setComment(extractComments(new File(proj.getLocation().toString(), "README.md")));
				proj.setDescription(description, monitor);
				license = extractLicense(new File(proj.getLocation().toString(), "LICENSE"));
			}
			
			
			addFileToProject("settings/org.eclipse.jdt.core.prefs", ".settings/org.eclipse.jdt.core.prefs",
					container, monitor);
			addFileToProject("settings/org.eclipse.core.runtime.prefs", ".settings/org.eclipse.core.runtime.prefs",
					container, monitor);

			
			addFileToProject("AndroidManifest.xml", "AndroidManifest.xml",
					KEY_PROJECT_NAME, projectName,
					KEY_PACKAGE_NAME, packageName, KEY_API_LEVEL,
					targetApi.substring(targetApi.length() - 2),
					container, monitor);
			
			addFileToProject("ant.properties", "ant.properties",
					KEY_PROJECT_NAME, projectName,
					container, monitor);
			addFileToProject("build-evolution.bat", "build-evolution.bat", container, monitor);
			addFileToProject("build-release.bat", "build-release.bat",
					KEY_PROJECT_NAME, projectName,
					container, monitor);
			addFileToProject("build.xml", "build.xml",
					KEY_PROJECT_NAME, projectName,
					container, monitor);

			addFileToProject("classpath", ".classpath", container, monitor);
			addFileToProject("gitignore", ".gitignore", container, monitor);
			addFileToProject("gource.cfg", "gource.cfg", container, monitor);
			
			addFileToProject("javadoc.xml", "javadoc.xml",
					KEY_PROJECT_NAME, projectName,
					KEY_PACKAGE_NAME, packageName,
					KEY_PACKAGE_PATH, pathPackage,
					container, monitor);

			addFileToProject("proguard-project.txt", "proguard-project.txt",
					KEY_PACKAGE_NAME, packageName,
					container, monitor);
			addFileToProject("project.properties", "project.properties",
					KEY_TARGET_API, targetApi,
					container, monitor);
			
			Map<String, String> environment = System.getenv();
			
			final String androidHome = environment.get("ANDROID_HOME");
			final String androidSdkHome = environment.get("ANDROID_SDK_HOME");
			
			String sdkPath = androidHome != null && androidHome.length() > 0 ?
					androidHome :
						androidSdkHome != null && androidSdkHome.length() > 0 ?
								androidSdkHome : "";
			final String winSeparator = Character.toString ((char) 92);	
			final String doubleSeparator = winSeparator + winSeparator + winSeparator + winSeparator;
			if (File.separator.equals(winSeparator))
				sdkPath = sdkPath.replace(winSeparator, doubleSeparator);
						
			addFileToProject("local.properties", "local.properties",
					KEY_SDK_PATH, sdkPath,
					container, monitor);
			
			/*inputStream = this.getClass().getResourceAsStream("root/keystore.sig");
			addFileToProject(container, new Path("keystore.sig"), inputStream, monitor);
			inputStream.close();*/
			
			addFileToProject("res/mipmap-ldpi/ic_launcher.png",
					"res/mipmap-ldpi/ic_launcher.png", container, monitor);
			
			addFileToProject("res/mipmap-mdpi/ic_launcher.png",
					"res/mipmap-mdpi/ic_launcher.png", container, monitor);
			
			addFileToProject("res/mipmap-hdpi/ic_launcher.png",
					"res/mipmap-hdpi/ic_launcher.png", container, monitor);
			
			addFileToProject("res/mipmap-xhdpi/ic_launcher.png",
					"res/mipmap-xhdpi/ic_launcher.png", container, monitor);
			
			addFileToProject("res/mipmap-xxhdpi/ic_launcher.png",
					"res/mipmap-xxhdpi/ic_launcher.png", container, monitor);
			
			addFileToProject("res/mipmap-xxxhdpi/ic_launcher.png",
					"res/mipmap-xxxhdpi/ic_launcher.png", container, monitor);
			
			addFileToProject("res/layout/activity_main.xml",
					"res/layout/activity_main.xml", container, monitor);
			
			addFileToProject("res/values/strings.xml", "res/values/strings.xml",
					KEY_PROJECT_NAME, projectName,
					container, monitor);
			addFileToProject("res/values/styles.xml", "res/values/styles.xml",
					KEY_PROJECT_NAME, projectName,
					container, monitor);
			addFileToProject("res/values/themes.xml", "res/values/themes.xml",
					KEY_PROJECT_NAME, projectName,
					container, monitor);

			final String userName = getUserName();
			final String date = getCurrentDate();
			
			System.out.println(license);
			System.out.println("Name: " + userName);
			System.out.println("Date: " + date);
			
			addFileToProject("src/package-info.java.src", "/src/" + pathPackage + "/package-info.java",
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					container, monitor);
			
			addFileToProject("src/App.java.src", "/src/" + pathPackage + "/App.java",
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					container, monitor);
			
			addFileToProject("src/app/package-info.java.src", "/src/" + pathPackage + "/app/package-info.java",
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					container, monitor);
			
			addFileToProject("src/app/MainActivity.java.src", "/src/" + pathPackage + "/app/MainActivity.java",
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					container, monitor);
			
			addFileToProject("src/utils/package-info.java.src", "/src/" + pathPackage + "/utils/package-info.java",
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					container, monitor);
			
			addFileToProject("src/utils/Utils.java.src", "/src/" + pathPackage + "/utils/Utils.java",
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					container, monitor);
			
			gitSupport.close(gitName, gitEmail, gitMessage);
			
		} finally {monitor.done();}
	}
	
	/**
	 * Create directory-structure for the new project.
	 * 
	 * @param packageName the name of package
	 * @param project project container
	 * @param monitor for async-operation
	 * 
	 * @return path to root sources
	 * @throws CoreException causes if anything was failed
	 **/
	private final String createFolders(String packageName, IContainer project, IProgressMonitor monitor)
			throws CoreException {
		
		for (int i = 0; i < FOLDERS.length; i++)
			createFolder(FOLDERS[i], project, monitor);
		
		final String packagePath = createSrcFolder(packageName, project, monitor);
		
		for (int i = 0; i < SRC_FOLDERS.length; i++)
			//createSrcFolder(packagePath, SRC_FOLDERS[i], project, monitor);
			createFolder(packagePath + File.separator + SRC_FOLDERS[i], project, monitor);
		
		return packagePath;
		
	}
	
	/**
	 * Create folder in project.
	 * 
	 * @param folderPath new folder path
	 * @param project project container
	 * @param monitor for async-operation
	 * 
	 * @throws CoreException causes if anything was failed
	 */
	private final void createFolder(String folderPath, IContainer project, IProgressMonitor monitor)
			throws CoreException {
		project.getFolder(new Path(folderPath))
		.create(true, true, monitor);
	}
	
	/**
	 * Create src folder by package name. 
	 * 
	 * @param packageName project package name
	 * @param project project container
	 * @param monitor for async-operation
	 * 
	 * @return path to src-root
	 * @throws CoreException causes if anything was failed
	 **/
	private final String createSrcFolder(String packageName,
			IContainer project,	IProgressMonitor monitor) throws CoreException {
		final String packageFolders[] = packageName.split("\\.");
		String packagePath = "src";
		for (int i = 0; i < packageFolders.length; i++) {
			packagePath = packagePath + File.separator + packageFolders[i]; 
			project.getFolder(new Path(packagePath)).create(true, true, monitor);
		}
		return packagePath;
	}
	
	/**
	 * @param command command for execute os-script
	 * @return result of executing
	 * @throws CoreException causes when anything was failed
	 */
	@SuppressWarnings("unused")
	private static final int exec(String command) throws CoreException {
		int result = -1;
		try {result = Runtime.getRuntime().exec(command).waitFor();}
		catch (IOException | InterruptedException e) {callCrash(e);}
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 *      org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
		this.workbench = workbench;
	}

	/** Sets the initialization data for the wizard. */
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		this.config = config;
	}
	
	/** Adds a new file to the project. */
	/**
	 * Put new file to project.
	 * 
	 * @param filePath		path of file
	 * @param contentStream	content of file
	 * 
	 * @param project project container
	 * @param monitor for async-operation
	 * @throws CoreException causes if anything was failed
	 */
	private final void addFileToProject(String filePath, InputStream contentStream,
			IContainer project, IProgressMonitor monitor)
			throws CoreException {
		try {logln(filePath + " - " + contentStream.available());}
		catch (IOException e) {logln(e.getLocalizedMessage());}
		project.getFile(new Path(filePath)).create(contentStream, true, monitor);
	}
	
	/**
	 * Put new file to project by key-values.
	 * 
	 * @param dstFile		path of file
	 * @param contentStream	content of file
	 * 
	 * @param project project container
	 * @param monitor for async-operation
	 * @throws CoreException causes if anything was failed
	 */
	private final void addFileToProject(String srcFile, String dstFile,
			IContainer project, IProgressMonitor monitor)
			throws CoreException {
		try {
			final InputStream inputStream = Activator.getDefault().getBundle().getEntry("/files/" + srcFile).openStream();
			addFileToProject(dstFile, inputStream, project, monitor);
			try {inputStream.close();} catch (IOException e) {callCrash(e);}
		} catch (IOException e) {callCrash(e);}
	}

	
	/**
	 * Put new file to project by key-values.
	 * 
	 * @param srcFile source file name
	 * @param dstFile destination file name
	 * 
	 * @param project project container
	 * @param monitor for async-operation
	 * @throws CoreException causes if anything was failed
	 */
	private final void addFileToProject(String srcFile, String dstFile,
			String key, String value,
			IContainer project, IProgressMonitor monitor)
			throws CoreException {
		try {
			final InputStream inputStream = openContentStream(key, value,
					Activator.getDefault().getBundle().getEntry("/files/" + srcFile).openStream());
			addFileToProject(dstFile, inputStream, project, monitor);
			try {inputStream.close();} catch (IOException e) {callCrash(e);}
		} catch (IOException e) {callCrash(e);}
	}
	
	/**
	 * Put new file to project by key-values.
	 * 
	 * @param srcFile source file name
	 * @param dstFile destination file name
	 * 
	 * @param project project container
	 * @param monitor for async-operation
	 * @throws CoreException causes if anything was failed
	 */
	private final void addFileToProject(String srcFile, String dstFile,
			String key1, String value1,
			String key2, String value2,
			String key3, String value3,
			IContainer project, IProgressMonitor monitor)
			throws CoreException {
		try {
			final InputStream inputStream = openContentStream(key1, value1, key2, value2, key3, value3,
					Activator.getDefault().getBundle().getEntry("/files/" + srcFile).openStream());
			addFileToProject(dstFile, inputStream, project, monitor);
			try {inputStream.close();} catch (IOException e) {callCrash(e);}
		} catch (IOException e) {callCrash(e);}
	}
	
	/**
	 * Put new file to project by key-values.
	 * 
	 * @param srcFile source file name
	 * @param dstFile destination file name
	 * 
	 * @param project project container
	 * @param monitor for async-operation
	 * @throws CoreException causes if anything was failed
	 */
	private final void addFileToProject(String srcFile, String dstFile,
			String key1, String value1,
			String key2, String value2,
			String key3, String value3,
			String key4, String value4,
			String key5, String value5,
			IContainer project, IProgressMonitor monitor)
			throws CoreException {
		try {
			final InputStream inputStream = openContentStream(key1, value1, key2, value2, key3, value3,
					key4, value4, key5, value5,
					Activator.getDefault().getBundle().getEntry("/files/" + srcFile).openStream());
			addFileToProject(dstFile, inputStream, project, monitor);
			try {inputStream.close();} catch (IOException e) {callCrash(e);}
		} catch (IOException e) {callCrash(e);}
	}
	
	/**
	 * Initialize the file contents to contents of the given resource.
	 */
	public static InputStream openContentStream(String key, String value, InputStream is) throws CoreException {

		/* We want to be truly OS-agnostic */
		final String newline = System.getProperty("line.separator");
		
		final StringBuffer sb = new StringBuffer();	String line = null;
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			try {
				while ((line = reader.readLine()) != null) {
					line = line.replaceAll("\\$\\{" + key + "\\}", value);
					sb.append(line);
					sb.append(newline);
				}
			} finally {reader.close();}
		} catch (IOException e) {callCrash(e);}
		return new ByteArrayInputStream(sb.toString().getBytes());
	}
	
	/**
	 * Initialize the file contents to contents of the given resource.
	 */
	public static InputStream openContentStream(String key1, String value1, String key2, String value2, String key3, String value3, InputStream is) throws CoreException {

		/* We want to be truly OS-agnostic */
		final String newline = System.getProperty("line.separator");

		
		final StringBuffer sb = new StringBuffer();	String line = null;
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			try {
				while ((line = reader.readLine()) != null) {
					line = line
							.replaceAll("\\$\\{" + key1 + "\\}", value1)
							.replaceAll("\\$\\{" + key2 + "\\}", value2)
							.replaceAll("\\$\\{" + key3 + "\\}", value3);
					sb.append(line);
					sb.append(newline);
				}
			} finally {reader.close();}
		} catch (IOException e) {callCrash(e);}
		return new ByteArrayInputStream(sb.toString().getBytes());
	}
	
	/**
	 * Initialize the file contents to contents of the given resource.
	 */
	public static InputStream openContentStream(
			String key1, String value1,
			String key2, String value2,
			String key3, String value3,
			String key4, String value4,
			String key5, String value5,
			InputStream is) throws CoreException {

		/* We want to be truly OS-agnostic */
		final String newline = System.getProperty("line.separator");

		
		final StringBuffer sb = new StringBuffer();	String line = null;
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			try {
				while ((line = reader.readLine()) != null) {
					line = line
							.replaceAll("\\$\\{" + key1 + "\\}", value1)
							.replaceAll("\\$\\{" + key2 + "\\}", value2)
							.replaceAll("\\$\\{" + key3 + "\\}", value3)
							.replaceAll("\\$\\{" + key4 + "\\}", value4)
							.replaceAll("\\$\\{" + key5 + "\\}", value5);
					sb.append(line);
					sb.append(newline);
				}
			} finally {reader.close();}
		} catch (IOException e) {callCrash(e);}
		return new ByteArrayInputStream(sb.toString().getBytes());
	}


	/**
	 * @param name the name of console
	 * @return founded console
	 */
	private final MessageConsole findConsole(String name) {
	      final ConsolePlugin plugin = ConsolePlugin.getDefault();
	      final IConsoleManager conMan = plugin.getConsoleManager();
	      final IConsole[] existing = conMan.getConsoles();
	      for (int i = 0; i < existing.length; i++)
	         if (name.equals(existing[i].getName()))
	            return (MessageConsole) existing[i];
	      //no console found, so create a new one
	      final MessageConsole myConsole = new MessageConsole(name, null);
	      conMan.addConsoles(new IConsole[]{myConsole});
	      return myConsole;
	}

	/**	Show crash-alert */
	private static final void callCrash(Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, "Can't create project",
						IStatus.OK, exception.getLocalizedMessage(), exception));
	}

	/** @param message the message for log to current line */
	@SuppressWarnings("unused")
	private final void log(String message) {
		if (mMessageConsoleStream == null) return;
		mMessageConsoleStream.print(message);
	}

	/** @param message the message for log to new line */
	private final void logln(String message) {
		if (mMessageConsoleStream == null) return;
		mMessageConsoleStream.println(message);
	}

	/**	Insert empty line to console */
	@SuppressWarnings("unused")
	private final void logln() {
		if (mMessageConsoleStream == null) return;
		mMessageConsoleStream.println();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	@Override
	public final void dispose() {
		try {mMessageConsoleStream.close();}
		catch (IOException e) {e.printStackTrace();}
		mMessageConsoleStream = null;
		super.dispose();
	}
	
	/**
	 * @param tempProject name of temp project folder
	 * @return Comments, extracted from readme-file
	 * @throws CoreException causes when anything was failed
	 */
	private static final String extractComments(File file) throws CoreException {
		final StringBuilder builder = new StringBuilder();
		InputStreamReader inputStreamReader = null; BufferedReader bufferedReader = null;
		try {
			inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			bufferedReader = new BufferedReader(inputStreamReader);
			bufferedReader.readLine();
			String line = null;
			while ((line = bufferedReader.readLine()) != null)
				builder.append(line);
			
		} catch (IOException e) {callCrash(e);}
		finally {
			if (inputStreamReader != null)
				try {inputStreamReader.close();} catch (IOException e) {callCrash(e);}
			if (bufferedReader != null)
				try {bufferedReader.close();} catch (IOException e) {callCrash(e);}
	
		};
		
		return builder.toString();
	}

	/**
	 * @param tempProject name of temp project folder
	 * @return License, extracted from license-file
	 * @throws CoreException causes when anythig was failed
	 */
	private static final String extractLicense(File file) throws CoreException {
		/* We want to be truly OS-agnostic */
		final String newline = System.getProperty("line.separator");
		final StringBuilder builder = new StringBuilder();
		InputStreamReader inputStreamReader = null; BufferedReader bufferedReader = null;
		try {
			inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			bufferedReader = new BufferedReader(inputStreamReader);
			builder.append(" *  ");
			builder.append(newline);
			/*builder.append("/*"); builder.append(newline);
			builder.append(" *	"); builder.append(javaFileName); builder.append(newline);
			builder.append(" *	"); builder.append(projectName); builder.append(newline);
			builder.append(" *"); builder.append(bufferedReader.readLine()); builder.append(newline);
			builder.append(" *  "); builder.append(bufferedReader.readLine()); builder.append(newline);
			builder.append(" *"); builder.append(bufferedReader.readLine()); builder.append(newline);
			*/String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				builder.append(" *  ");
				builder.append(line);
				builder.append(newline);
			}
			//builder.append(" */");
		} catch (IOException e) {callCrash(e);}
		finally {
			if (inputStreamReader != null)
				try {inputStreamReader.close();} catch (IOException e) {callCrash(e);}
			if (bufferedReader != null)
				try {bufferedReader.close();} catch (IOException e) {callCrash(e);}
	
		};
		
		return builder.toString();
	}


	/** @return Default user name specified as runtime eclipse option. */
	/** @return current user name */
	private static final String getUserName() throws CoreException{
		String line = null;
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					new File(new File(Platform.getInstallLocation().getURL().toURI()),
							"eclipse.ini").getAbsolutePath()));
			try {
				while ((line = reader.readLine()) != null)
					if (line.startsWith("-Duser.name="))
						return line.substring(line.indexOf("=") + 1);
			} finally {reader.close();}
		} catch (IOException | URISyntaxException e) {callCrash(e);}
		return "";
	}
	
	/** @return Default user name specified as runtime eclipse option. */
	/** @return current user language. */
	private static final String getLanguage() throws CoreException {
		String line = null;
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					new File(new File(Platform.getInstallLocation().getURL().toURI()),
							"eclipse.ini").getAbsolutePath()));
			try {
				while ((line = reader.readLine()) != null)
					if (line.startsWith("-Duser.language="))
						return line.substring(line.indexOf("=") + 1);
			} finally {reader.close();}
		} catch (IOException | URISyntaxException e) {callCrash(e);}
		return "";
	}

	/** @return current date stamp for comment sources. */
	/** @return current time stamp in string format.	 */
	private static final String getCurrentDate() throws CoreException {
		return new SimpleDateFormat("MMM dd, yyyy",
				new Locale(getLanguage()))
				.format(new Date(System.currentTimeMillis()));
	}
	
		

}