package ru.nikitenkogleb.wizards.android.app;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.core.runtime.*;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.*;

import java.io.*;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.wb.swt.ResourceManager;

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
IExecutableExtension {
	
	
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
	
	/**	String preffix for temp project directory. */
	private static final String TEMP_PREFFIX = "_temp";
	
	/**	Android list targets command line */
	private static final String EXEC_ANDROID_LIST_TARGETS = "android.bat list targets";
	
	/** Windows-specific clone operations. */
	private static final String CLONE_COMMAND_WINDOWS =
			"cmd /c start /wait %s %s %s %s";
	/** Windows-specific commit operations. */
	private static final String COMMIT_COMMAND_WINDOWS =
			"cmd /c start /wait %s %s";
	
	private static final String REGEXP_PACKAGE_NAME =
			"([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*";
	
	private static final Locale LOCALE = Locale.getDefault();
		
	private String mCloneBatFile = null;
	private String mCommitBatFile = null;
	
	/*
	 * Use the WizardNewProjectCreationPage, which is provided by the Eclipse
	 * framework.
	 */
	private WizardNewProjectCreationPage wizardPage;

	private IConfigurationElement config;

	private IWorkbench workbench;

	@SuppressWarnings("unused")
	private IStructuredSelection selection;

	private IProject project;
	
	private Text packageNameText = null;
	private Text gitRepositoryText = null;
	private Combo targetApiCombo = null;
	
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
	public EmptyAppWizard() {super();}
	
	/** Adding the page to the wizard. */
	public void addPages() {
		
		/*
		 * Unlike the custom new wizard, we just add the pre-defined one and
		 * don't necessarily define our own.
		 */
		wizardPage = new WizardNewProjectCreationPage("NewExampleComSiteProject") {
			@Override
			public void createControl(Composite parent) {
				super.createControl(parent);
				parent = (Composite) getControl();
				Composite container = new Composite(parent, SWT.NONE);
				setControl(parent);
				
				GridLayout gl_container = new GridLayout(4, false);
				gl_container.verticalSpacing = 8;
				gl_container.marginWidth = 8;
				gl_container.marginHeight = 8;
				gl_container.horizontalSpacing = 8;
				container.setLayout(gl_container);
				
				Label packageNameLabel = new Label(container, SWT.NONE);
				packageNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
				packageNameLabel.setText("&Package name:");
				
				packageNameText = new Text(container, SWT.BORDER);
				GridData gd_packageNameText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
				gd_packageNameText.widthHint = 209;
				packageNameText.setText("com.example");
				packageNameText.setLayoutData(gd_packageNameText);
				packageNameText.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {dialogChanged();}
				});
				
				Label targetApiLabel = new Label(container, SWT.NONE);
				targetApiLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
				targetApiLabel.setText("&Target Api:");
				
				targetApiCombo  = new Combo(container, SWT.READ_ONLY);
				final String[] apiTargets = getApiTargets();
				targetApiCombo.setItems(apiTargets);
				targetApiCombo.select(apiTargets.length - 1);
				
				Label gitRepositoryLabel = new Label(container, SWT.NONE);
				gitRepositoryLabel.setText("&Git repository:");
				
				gitRepositoryText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
				gitRepositoryText.setEnabled(false);
				GridData gd_gitRepositoryText = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
				gd_gitRepositoryText.widthHint = 277;
				gitRepositoryText.setLayoutData(gd_gitRepositoryText);
				
				Button pasteFromClipboardButton = new Button(container, SWT.NONE);
				pasteFromClipboardButton.setText("Paste from &clipboard");
				pasteFromClipboardButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
				pasteFromClipboardButton.setImage(ResourceManager.getPluginImage("ru.nikitenkogleb.wizards.android.app", "icons/paste.png"));
				final Clipboard cb = new Clipboard(getShell().getDisplay());
				pasteFromClipboardButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						final TextTransfer transfer = TextTransfer.getInstance();
						String data = (String)cb.getContents(transfer);
						if (data != null)
							try {
								if (!data.startsWith("git@"))
									new URL(data).openConnection().connect();
								gitRepositoryText.setText(data);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
					}
				});
				
			}
		};
		
		wizardPage.setTitle("New Android Application");
		wizardPage.setInitialProjectName("MyApplication");
		wizardPage.setDescription("This wizard creates a new empty Android Application.\nAlso you can specify existing GIT-repository for CVS-integration.");
		wizardPage.setImageDescriptor(ResourceManager.getPluginImageDescriptor("ru.nikitenkogleb.wizards.android.app", "icons/android_app.png"));
		
		addPage(wizardPage);
		
		try {
			mCloneBatFile = FileLocator.toFileURL(this.getClass()
					.getResource("files/clone.bat")).getFile().substring(1);
			mCommitBatFile = FileLocator.toFileURL(this.getClass()
					.getResource("files/commit.bat")).getFile().substring(1);
		} catch (IOException e) {e.printStackTrace();}
		
	}
		
	/**
	 * Validates the fields on the form. If you add more fields that are
	 * required, make sure to put the checks in here.
	 */
	private void dialogChanged() {
		final String fileName = wizardPage.getProjectHandle().getName();
		final IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(fileName));

		if (container != null
				&& ((container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 1)) {
			updateStatus("Project " + fileName.substring(1) + " already exist");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("Project name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("Project name must be valid");
			return;
		}
		
		final String packageName = packageNameText.getText();
		if (packageName.length() == 0) {
			updateStatus("Package name must be specified");
			return;
		}
		
		if (!packageName.matches(REGEXP_PACKAGE_NAME)) {
			updateStatus("Project name must be valid");
			return;
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		wizardPage.setErrorMessage(message);
		wizardPage.setPageComplete(message == null);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		
		if (project != null) return true;

		final IProject projectHandle = wizardPage.getProjectHandle();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		
		
		final String projectName = projectHandle.getName();
		final String tempProject = projectName + TEMP_PREFFIX;
		final String projectNameLowerCase = projectName.toLowerCase(Locale.getDefault());
		final String packageName = packageNameText.getText() + "." +
				projectNameLowerCase;
		final String targetApi = targetApiCombo.getText();
		final String gitRepository = gitRepositoryText.getText();
		
		final IProjectDescription description = workspace
				.newProjectDescription(projectHandle.getName());
		
		final URI projectURI = (!wizardPage.useDefaults()) ? wizardPage
				.getLocationURI() : null;
				
				
				
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
						targetApi, tempProject, gitRepository);
			}
		};

		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {return false;}
		catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException
					.getMessage());
			return false;
		}

		project = projectHandle;

		if (project == null) return false;
		
		BasicNewProjectResourceWizard.updatePerspective(config);
		BasicNewProjectResourceWizard.selectAndReveal(project, workbench
				.getActiveWorkbenchWindow());

		return true;
	}

	
	/** This creates the project in the workspace. */
	void createProject(IProjectDescription description, IProject proj, IProgressMonitor monitor,
			String projectName,
			String projectNameLowerCase,
			String packageName,
			String targetApi,
			String tempProject,
			String gitRepository) throws CoreException,
			OperationCanceledException {
		try {
			monitor.beginTask("", 2000);
			
			//final String tempProjectPath = new File(tempProject).getAbsolutePath();
			
			proj.create(description, new SubProgressMonitor(monitor, 1000));
			
			proj.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(
					monitor, 1000));
			
			try {proj.setDefaultCharset("UTF-8", monitor);}
			catch (CoreException e) {e.printStackTrace();}
			
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			/*
			 * Okay, now we have the project and we can do more things with it
			 * before updating the perspective.
			 */
			final IContainer container = (IContainer) proj;
			InputStream inputStream = null;
			
			final IFolder settingsFolder = container.getFolder(new Path(".settings"));
			if (!settingsFolder.exists())
				settingsFolder.create(true, true, monitor);
			
			final IFolder srcFolder = container.getFolder(new Path("src"));
			if (!srcFolder.exists())
				srcFolder.create(true, true, monitor);

			final IFolder resFolder = container.getFolder(new Path("res"));
			if (!resFolder.exists())
				resFolder.create(true, true, monitor);
			
			final IFolder binFolder = container.getFolder(new Path("bin"));
			if (!binFolder.exists())
				binFolder.create(true, true, monitor);
			
			final IFolder genFolder = container.getFolder(new Path("gen"));
			if (!genFolder.exists())
				genFolder.create(true, true, monitor);
			
			final IFolder mipmapLdpiFolder = container.getFolder(new Path("res/mipmap-ldpi"));
			if (!mipmapLdpiFolder.exists())
				mipmapLdpiFolder.create(true, true, monitor);
			
			final IFolder mipmapMdpiFolder = container.getFolder(new Path("res/mipmap-mdpi"));
			if (!mipmapMdpiFolder.exists())
				mipmapMdpiFolder.create(true, true, monitor);
			
			final IFolder mipmapHdpiFolder = container.getFolder(new Path("res/mipmap-hdpi"));
			if (!mipmapHdpiFolder.exists())
				mipmapHdpiFolder.create(true, true, monitor);
			
			final IFolder mipmapXHdpiFolder = container.getFolder(new Path("res/mipmap-xhdpi"));
			if (!mipmapXHdpiFolder.exists())
				mipmapXHdpiFolder.create(true, true, monitor);
			
			final IFolder mipmapXXHdpiFolder = container.getFolder(new Path("res/mipmap-xxhdpi"));
			if (!mipmapXXHdpiFolder.exists())
				mipmapXXHdpiFolder.create(true, true, monitor);
			
			final IFolder mipmapXXXHdpiFolder = container.getFolder(new Path("res/mipmap-xxxhdpi"));
			if (!mipmapXXXHdpiFolder.exists())
				mipmapXXXHdpiFolder.create(true, true, monitor);

			final String packageFolders[] = packageName.split("\\.");
			String packagePath = "src";
			for (int i = 0; i < packageFolders.length; i++) {
				packagePath = packagePath + File.separator + packageFolders[i]; 
				final IFolder folder = container.getFolder(new Path(packagePath));
				if (!folder.exists()) folder.create(true, true, monitor);
			}
			
			final IFolder appFolder = container.getFolder(new Path(packagePath + "/app"));
			if (!appFolder.exists()) appFolder.create(true, true, monitor);
			final IFolder utilsFolder = container.getFolder(new Path(packagePath + "/utils"));
			if (!utilsFolder.exists()) utilsFolder.create(true, true, monitor);
			
			final IFolder layoutFolder = container.getFolder(new Path("res/layout"));
			if (!layoutFolder.exists())
				layoutFolder.create(true, true, monitor);
			
			final IFolder valuesFolder = container.getFolder(new Path("res/values"));
			if (!valuesFolder.exists())
				valuesFolder.create(true, true, monitor);

			inputStream = this.getClass().getResourceAsStream("files/classpath");
			addFileToProject(container, new Path(".classpath"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/settings/org.eclipse.jdt.core.prefs");
			addFileToProject(container, new Path(settingsFolder.getName()
					+ Path.SEPARATOR + "org.eclipse.jdt.core.prefs"),
					inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/.settings/org.eclipse.core.runtime.prefs");
			addFileToProject(container, new Path(settingsFolder.getName()
					+ Path.SEPARATOR + "org.eclipse.core.runtime.prefs"),
					inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/gitignore");
			addFileToProject(container, new Path(".gitignore"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/project.properties");
			inputStream = openContentStream(KEY_TARGET_API, targetApi, inputStream);
			addFileToProject(container, new Path("project.properties"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/proguard-project.txt");
			inputStream = openContentStream(KEY_PACKAGE_NAME, packageName, inputStream);
			addFileToProject(container, new Path("proguard-project.txt"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/ant.properties");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName, inputStream);
			addFileToProject(container, new Path("ant.properties"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/build.xml");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName, inputStream);
			addFileToProject(container, new Path("build.xml"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/build-evolution.bat");
			addFileToProject(container, new Path("build-evolution.bat"), inputStream, monitor);
			inputStream.close();

			inputStream = this.getClass().getResourceAsStream("files/build-release.bat");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName, inputStream);
			addFileToProject(container, new Path("build-release.bat"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/gource.cfg");
			addFileToProject(container, new Path("gource.cfg"), inputStream, monitor);
			inputStream.close();
			
			final String pathPackage = packageName.replace(".", "/");
			
			inputStream = this.getClass().getResourceAsStream("files/javadoc.xml");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName,
					KEY_PACKAGE_NAME, packageName, KEY_PACKAGE_PATH, pathPackage, inputStream);
			addFileToProject(container, new Path("javadoc.xml"), inputStream, monitor);
			inputStream.close();
			
			/*inputStream = this.getClass().getResourceAsStream("files/keystore.sig");
			addFileToProject(container, new Path("keystore.sig"), inputStream, monitor);
			inputStream.close();*/
			
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
						
			inputStream = this.getClass().getResourceAsStream("files/local.properties");
			inputStream = openContentStream(KEY_SDK_PATH, sdkPath, inputStream);
			addFileToProject(container, new Path("local.properties"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/mipmap-ldpi/ic_launcher.png");
			addFileToProject(container, new Path("res/mipmap-ldpi/ic_launcher.png"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/mipmap-mdpi/ic_launcher.png");
			addFileToProject(container, new Path("res/mipmap-mdpi/ic_launcher.png"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/mipmap-hdpi/ic_launcher.png");
			addFileToProject(container, new Path("res/mipmap-hdpi/ic_launcher.png"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/mipmap-xhdpi/ic_launcher.png");
			addFileToProject(container, new Path("res/mipmap-xhdpi/ic_launcher.png"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/mipmap-xxhdpi/ic_launcher.png");
			addFileToProject(container, new Path("res/mipmap-xxhdpi/ic_launcher.png"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/mipmap-xxxhdpi/ic_launcher.png");
			addFileToProject(container, new Path("res/mipmap-xxxhdpi/ic_launcher.png"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/layout/activity_main.xml");
			addFileToProject(container, new Path("res/layout/activity_main.xml"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/res/values/strings.xml");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName, inputStream);
			addFileToProject(container, new Path("res/values/strings.xml"), inputStream, monitor);
			inputStream.close();

			inputStream = this.getClass().getResourceAsStream("files/res/values/styles.xml");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName, inputStream);
			addFileToProject(container, new Path("res/values/styles.xml"), inputStream, monitor);
			inputStream.close();

			inputStream = this.getClass().getResourceAsStream("files/res/values/themes.xml");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName, inputStream);
			addFileToProject(container, new Path("res/values/themes.xml"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/AndroidManifest.xml");
			inputStream = openContentStream(KEY_PROJECT_NAME, projectName,
					KEY_PACKAGE_NAME, packageName, KEY_API_LEVEL,
					targetApi.substring(targetApi.length() - 2), inputStream);
			addFileToProject(container, new Path("AndroidManifest.xml"), inputStream, monitor);
			inputStream.close();

			String license = "";
			if(gitRepository != null && gitRepository.length() != 0) {
				System.out.println("Result: " +
						exec(String.format(LOCALE, CLONE_COMMAND_WINDOWS,
								mCloneBatFile, gitRepository, tempProject,
								proj.getLocation().toString())));
					
				description = proj.getDescription();
				description.setComment(extractComments(new File(proj.getLocation().toString(), "README.md")));
				proj.setDescription(description, monitor);
				
				license = extractLicense(new File(proj.getLocation().toString(), "LICENSE"));
				
			}
			
			final String userName = getUserName();
			final String date = getCurrentDate();
			
			System.out.println(license);
			System.out.println("Name: " + userName);
			System.out.println("Date: " + date);
			
			inputStream = this.getClass().getResourceAsStream("files/src/package-info.java.src");
			inputStream = openContentStream(
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					inputStream);
			addFileToProject(container, new Path(packagePath + "/package-info.java"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/src/App.java.src");
			inputStream = openContentStream(
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					inputStream);
			addFileToProject(container, new Path(packagePath + "/App.java"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/src/app/package-info.java.src");
			inputStream = openContentStream(
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					inputStream);
			addFileToProject(container, new Path(packagePath + "/app/package-info.java"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/src/app/MainActivity.java.src");
			inputStream = openContentStream(
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					inputStream);
			addFileToProject(container, new Path(packagePath + "/app/MainActivity.java"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/src/utils/package-info.java.src");
			inputStream = openContentStream(
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					inputStream);
			addFileToProject(container, new Path(packagePath + "/utils/package-info.java"), inputStream, monitor);
			inputStream.close();
			
			inputStream = this.getClass().getResourceAsStream("files/src/utils/Utils.java.src");
			inputStream = openContentStream(
					KEY_PROJECT_NAME, projectName,
					KEY_LICENSE, license,
					KEY_DATE, date,
					KEY_PACKAGE_NAME, packageName,
					KEY_AUTHOR, userName,
					inputStream);
			addFileToProject(container, new Path(packagePath + "/utils/Utils.java"), inputStream, monitor);
			inputStream.close();
			
			if(gitRepository != null && gitRepository.length() != 0)
				System.out.println("Result: " +
						exec(String.format(LOCALE, COMMIT_COMMAND_WINDOWS,
								mCommitBatFile,
								proj.getLocation().toString())));
			
		} catch (Exception ioe) {
			IStatus status = new Status(IStatus.ERROR, "ru.nikitenkogleb.wizards.android.app", IStatus.OK,
					ioe.getMessage(), ioe);
			throw new CoreException(status);
		} finally {
			monitor.done();
		}
	}
	
	
	
	/**
	 * @param tempProject name of temp project folder
	 * @return Comments, extracted from readme-file
	 */
	private static final String extractComments(File file) {
		final StringBuilder builder = new StringBuilder();
		InputStreamReader inputStreamReader = null; BufferedReader bufferedReader = null;
		try {
			inputStreamReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			bufferedReader = new BufferedReader(inputStreamReader);
			bufferedReader.readLine();
			String line = null;
			while ((line = bufferedReader.readLine()) != null)
				builder.append(line);
			
		} catch (IOException e) {e.printStackTrace();}
		finally {
			if (inputStreamReader != null)
				try {inputStreamReader.close();} catch (IOException e) {e.printStackTrace();}
			if (bufferedReader != null)
				try {bufferedReader.close();} catch (IOException e) {e.printStackTrace();}

		};
		
		return builder.toString();
	}
	
	/**
	 * @param tempProject name of temp project folder
	 * @return License, extracted from license-file
	 */
	private static final String extractLicense(File file) {
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
		} catch (IOException e) {e.printStackTrace();}
		finally {
			if (inputStreamReader != null)
				try {inputStreamReader.close();} catch (IOException e) {e.printStackTrace();}
			if (bufferedReader != null)
				try {bufferedReader.close();} catch (IOException e) {e.printStackTrace();}

		};
		
		return builder.toString();
	}

	/**
	 * @param command command for execute os-script
	 * @return result of executing
	 */
	private static final int exec(String command) {
		int result = -1;
		try {result = Runtime.getRuntime().exec(command).waitFor();}
		catch (IOException | InterruptedException e) {e.printStackTrace();}
		return result;
	}
	
	/** @return available android api targets */
	private static final String[] getApiTargets() {
		
		String[] result = null;
		try {
			final Process process =
					
					//Runtime.getRuntime().exec("cmd /c start /wait " + EXEC_ANDROID_LIST_TARGETS);
					Runtime.getRuntime().exec(EXEC_ANDROID_LIST_TARGETS);
			process.waitFor();
			final BufferedReader bufferedReader =
					new BufferedReader(new InputStreamReader(process.getInputStream()));
			final ArrayList<String> targets = new ArrayList<String>();
			String line = null;
			try {
				while ((line = bufferedReader.readLine()) != null)
					if (line.startsWith("id: "))
						targets.add(line.substring(
								line.indexOf("\"") + 1, line.length() - 1));
				result = targets.toArray(new String[targets.size()]);
			} catch (IOException e) {e.printStackTrace();}
		      
		    bufferedReader.close();
		} catch (IOException | InterruptedException e) {e.printStackTrace();}
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
	private void addFileToProject(IContainer container, Path path,
			InputStream contentStream, IProgressMonitor monitor)
			throws CoreException {
		final IFile file = container.getFile(path);
		if (file.exists())
			file.setContents(contentStream, true, true, monitor);
		else
			file.create(contentStream, true, monitor);
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
		} catch (IOException ioe) {
			IStatus status = new Status(IStatus.ERROR, "EmptyAppWizard", IStatus.OK,
					ioe.getLocalizedMessage(), null);
			throw new CoreException(status);
		}
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
		} catch (IOException ioe) {
			IStatus status = new Status(IStatus.ERROR, "EmptyAppWizard", IStatus.OK,
					ioe.getLocalizedMessage(), null);
			throw new CoreException(status);
		}
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
		} catch (IOException ioe) {
			IStatus status = new Status(IStatus.ERROR, "EmptyAppWizard", IStatus.OK,
					ioe.getLocalizedMessage(), null);
			throw new CoreException(status);
		}
		return new ByteArrayInputStream(sb.toString().getBytes());
	}


	/** @return Default user name specified as runtime eclipse option. */
	private static final String getUserName() {
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
		} catch (IOException | URISyntaxException ioe) {ioe.printStackTrace();}
		return "";
	}
	
	/** @return Default user name specified as runtime eclipse option. */
	private static final String getLanguage() {
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
		} catch (IOException | URISyntaxException ioe) {ioe.printStackTrace();}
		return "";
	}

	
	/** @return current date stamp for comment sources. */
	private static final String getCurrentDate() {
		return new SimpleDateFormat("MMM dd, yyyy",
				new Locale(getLanguage()))
				.format(new Date(System.currentTimeMillis()));
	}

}