/*
 * WizardPage.java
 */
package ru.nikitenkogleb.androidtools.newappwizard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.JFaceResources;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

/**
 * @author Nikitenko Gleb
 */
final class WizardPage extends WizardNewProjectCreationPage {
	

	/** Regular expression to validate package name field. */
	private static final String REGEXP_PACKAGE_NAME =
			"([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*";
	
	/** Regular expression to validate author email field. */
	private static final String REGEXP_EMAIL =
			"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
					+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	
	/**	Android list targets command line */
	private static final String EXEC_ANDROID_LIST_TARGETS = "android.bat list targets";
	
	/**	The name of wizard page. */
	private static final String PAGE_NAME = "New Android Application";
	/**	The initial project name. */
	private static final String INITIAL_PROJECT_NAME = "MyApplication";
	/**	The default wizard description. */
	private static final String DESCRIPTION =
			"This wizard creates a new empty Android Application.\n" +
			"Also you can specify existing GIT-repository for CVS-integration.";
	
	/**	The text of git repository paste button. */
	private static final String LABEL_PASTE_BUTTON = "Paste from clipboard";
	
	/**	The content of package name text label. */
	private static final String LABEL_PACKAGE_NAME = "Package name:";
	/**	The name of application package by default. */
	private static final String DEFAULT_PACKAGE_NAME = "ru.nikitenkogleb";
	/**	The name of application package (hint). */
	private static final String MESSAGE_PACKAGE_NAME = "For example: " + DEFAULT_PACKAGE_NAME;
	
	/**	The content of target api combo box label. */
	private static final String LABEL_TARGET_API = "Target Api:";
	/**	Default combo box text for correct layout measuring. */
	private static final String DEFAULT_TARGET_API = "Search installed api's...       ";
	
	
	/**	The content of git repository text label. */
	private static final String LABEL_GIT_REPOSITORY = "Git repository:";
	/**	The name of git repository by default. */
	private static final String DEFAULT_GIT_REPOSITORY = "";
	/**	The name of git repository (hint). */
	private static final String MESSAGE_GIT_REPOSITORY = "Link your project with existing git repository";

	/**	The content of git user name text label. */
	private static final String LABEL_GIT_USER_NAME = "User:";
	/**	The name of git user name by default. */
	private static final String DEFAULT_GIT_USER_NAME = "";
	/**	The name of git user name by default(hint). */
	private static final String MESSAGE_GIT_USER_NAME = "User name of your git-repository";

	/**	The content of git user password text label. */
	private static final String LABEL_GIT_USER_PASS_HTTPS = "Password:";
	/**	The content of git user password text label. */
	private static final String LABEL_GIT_USER_PASS_SSH = "SSH Passphrase:";

	/**	The name of git user password by default. */
	private static final String DEFAULT_GIT_USER_PASS = "";
	/**	The name of git user password by default(hint). */
	private static final String MESSAGE_GIT_USER_PASS = "Password / Passphrase (ssh)";

	/**	The content of git author name text label. */
	private static final String LABEL_GIT_AUTHOR_NAME = "Author name:";
	/**	The name of git author name by default. */
	private static final String DEFAULT_GIT_AUTHOR_NAME = "Gleb Nikitenko";
	/**	The name of git author name by default (hint). */
	private static final String MESSAGE_GIT_AUTHOR_NAME = "Author of initial commit";

	/**	The content of git author email text label. */
	private static final String LABEL_GIT_AUTHOR_EMAIL = "Author email:";
	/**	The name of git author email by default. */
	private static final String DEFAULT_GIT_AUTHOR_EMAIL = "gleb@e-magic.org";
	/**	The name of git author email by default (hint). */
	private static final String MESSAGE_GIT_AUTHOR_EMAIL = "Commit author's e-mail";

	/**	The content of git initial branch text label. */
	private static final String LABEL_GIT_INITIAL_BRANCH = "Initial Branch:";
	/**	The name of git initial branch by default. */
	private static final String DEFAULT_GIT_INITIAL_BRANCH = "dev";
	/**	The name of git initial branch by default (hint). */
	private static final String MESSAGE_GIT_INITIAL_BRANCH = "Initial branch name. For ex.: 'master', 'dev', etc...";

	/**	The content of git initial commit message text label. */
	private static final String LABEL_GIT_INITIAL_COMMIT_MESSAGE = "Initial Commit:";
	/**	The name of git initial commit message by default. */
	private static final String DEFAULT_GIT_INITIAL_COMMIT_MESSAGE = "Prepared for development";
	/**	The name of git initial commit message by default. */
	private static final String MESSAGE_GIT_INITIAL_COMMIT_MESSAGE = "Initial commit message";

	/**	The name of title icon file. */
	private static final String IMAGE_TITLE = "android_app.png";
	/**	The name of paste icon file. */
	private static final String IMAGE_PASTE = "paste.png";

	/** The callback of wizard */
	@SuppressWarnings("unused")
	private final WizardCallback mWizardCallback;
	
	/**	Package name text widget. */
	private Text mPackageNameText = null;
	/**	Target Api combo box widget. */
	private Combo mTargetApiCombo = null;
	/**	Git repository text widget. */
	private Text mGitRepositoryText = null;
	/**	Git user name text widget. */
	private Text mGitUserText = null;
	/**	Git user password text widget. */
	private Text mGitPassText = null;
	/**	Git author name text widget. */
	private Text mGitAuthorNameText = null;
	/**	Git author email text widget. */
	private Text mGitAuthorEmailText = null;
	/**	Git initial branch name text widget. */
	private Text mGitInitialBranchText = null;
	/**	Git author email text widget. */
	private Text mGitInitialCommitMessageText = null;

	/**	Git user name label widget. */
	private Label mGitUserLabel = null;
	/**	Git user password label widget. */
	private Label mGitPassLabel = null;
	/**	Git author name label widget. */
	private Label mGitAuthorNameLabel = null;
	/**	Git author email label widget. */
	private Label mGitAuthorEmailLabel = null;
	/**	Git initial branch name label widget. */
	private Label mGitInitialBranchLabel = null;
	/**	Git author email label widget. */
	private Label mGitInitialCommitMessageLabel = null;

	/**	System installed api targets */
	private String[] mApiTargets = null;

	/**
	 * @param wizardCallback {@link #mWizardCallback}
	 */
	public WizardPage(WizardCallback wizardCallback) {
		super(PAGE_NAME);
		mWizardCallback = wizardCallback;
		setTitle(PAGE_NAME);
		setInitialProjectName(INITIAL_PROJECT_NAME);
		setDescription(DESCRIPTION);
		setImageDescriptor(Activator.getImageDescriptor(IMAGE_TITLE));
		new Thread(new DetectInstalledTargets(new ReturnInstalledTargets(this)),
				"Search installed apis").start();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewProjectCreationPage
	 * #createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		parent = (Composite) getControl();
		
		final Composite container = new Composite(parent, SWT.NONE);
		setControl(parent);
		
		final GridLayout gl_container = new GridLayout(4, false);
		gl_container.verticalSpacing = 8; gl_container.marginWidth = 8;
		gl_container.marginHeight = 8; gl_container.horizontalSpacing = 8;
		container.setLayout(gl_container);
		
		final Label packageNameLabel = new Label(container, SWT.NONE);
		packageNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		packageNameLabel.setText(LABEL_PACKAGE_NAME);
		
		mPackageNameText = new Text(container, SWT.BORDER);
		final GridData gd_packageNameText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_packageNameText.widthHint = 209;
		mPackageNameText.setText(DEFAULT_PACKAGE_NAME);
		mPackageNameText.setMessage(MESSAGE_PACKAGE_NAME);
		mPackageNameText.setLayoutData(gd_packageNameText);
		mPackageNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		final Label targetApiLabel = new Label(container, SWT.NONE);
		targetApiLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		targetApiLabel.setText(LABEL_TARGET_API);
		
		mTargetApiCombo  = new Combo(container, SWT.READ_ONLY);
		mTargetApiCombo.setItems(DEFAULT_TARGET_API);
		mTargetApiCombo.select(0);
		mTargetApiCombo.setEnabled(false);
		invalidateApiTargets();
		
		Label gitRepositoryLabel = new Label(container, SWT.NONE);
		gitRepositoryLabel.setText(LABEL_GIT_REPOSITORY);
		
		mGitRepositoryText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		mGitRepositoryText.setEnabled(false);
		mGitRepositoryText.setText(DEFAULT_GIT_REPOSITORY);
		mGitRepositoryText.setMessage(MESSAGE_GIT_REPOSITORY);
		GridData gd_gitRepositoryText = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
		gd_gitRepositoryText.widthHint = 300;
		mGitRepositoryText.setLayoutData(gd_gitRepositoryText);
		
		Button pasteFromClipboardButton = new Button(container, SWT.NONE);
		pasteFromClipboardButton.setText(LABEL_PASTE_BUTTON);
		pasteFromClipboardButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		pasteFromClipboardButton.setImage(Activator
				.getImageDescriptor(IMAGE_PASTE).createImage());
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
						mGitRepositoryText.setText(data);
						updateGitSection();
					} catch (Exception exception) {exception.printStackTrace();}
			}
		});
		
		final Label divider = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		divider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		
		mGitUserLabel = new Label(container, SWT.NONE);
		mGitUserLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		mGitUserLabel.setText(LABEL_GIT_USER_NAME);

		mGitUserText = new Text(container, SWT.BORDER);
		mGitUserText.setText(DEFAULT_GIT_USER_NAME);
		mGitUserText.setMessage(MESSAGE_GIT_USER_NAME);
		GridData gd_gitUserName = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		//gd_gitUserText.widthHint = 277;
		mGitUserText.setLayoutData(gd_gitUserName);
		mGitUserText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		mGitPassLabel = new Label(container, SWT.NONE);
		mGitPassLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		mGitPassLabel.setText(LABEL_GIT_USER_PASS_HTTPS);
		
		mGitPassText = new Text(container, SWT.BORDER | SWT.PASSWORD);
		mGitPassText.setText(DEFAULT_GIT_USER_PASS);
		mGitPassText.setMessage(MESSAGE_GIT_USER_PASS);
		GridData gd_passwordText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		//gd_gitEmailText.widthHint = 277;
		mGitPassText.setLayoutData(gd_passwordText);
		mGitPassText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		mGitAuthorNameLabel = new Label(container, SWT.NONE);
		mGitAuthorNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		mGitAuthorNameLabel.setText(LABEL_GIT_AUTHOR_NAME);
		
		mGitAuthorNameText = new Text(container, SWT.BORDER);
		mGitAuthorNameText.setText(DEFAULT_GIT_AUTHOR_NAME);
		mGitAuthorNameText.setMessage(MESSAGE_GIT_AUTHOR_NAME);
		GridData gd_gitUserText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		//gd_gitUserText.widthHint = 277;
		mGitAuthorNameText.setLayoutData(gd_gitUserText);
		mGitAuthorNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		mGitAuthorEmailLabel = new Label(container, SWT.NONE);
		mGitAuthorEmailLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		mGitAuthorEmailLabel.setText(LABEL_GIT_AUTHOR_EMAIL);
		
		mGitAuthorEmailText = new Text(container, SWT.BORDER);
		mGitAuthorEmailText.setText(DEFAULT_GIT_AUTHOR_EMAIL);
		mGitAuthorEmailText.setMessage(MESSAGE_GIT_AUTHOR_EMAIL);
		GridData gd_gitEmailText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		//gd_gitEmailText.widthHint = 277;
		mGitAuthorEmailText.setLayoutData(gd_gitEmailText);
		mGitAuthorEmailText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		mGitInitialBranchLabel = new Label(container, SWT.NONE);
		mGitInitialBranchLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		mGitInitialBranchLabel.setText(LABEL_GIT_INITIAL_BRANCH);
		
		mGitInitialBranchText = new Text(container, SWT.BORDER);
		mGitInitialBranchText.setText(DEFAULT_GIT_INITIAL_BRANCH);
		mGitInitialBranchText.setMessage(MESSAGE_GIT_INITIAL_BRANCH);
		GridData gd_gitInitialBranchText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		//gd_gitInitialBranchText.widthHint = 277;
		mGitInitialBranchText.setLayoutData(gd_gitInitialBranchText);
		mGitInitialBranchText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		mGitInitialCommitMessageLabel = new Label(container, SWT.NONE);
		mGitInitialCommitMessageLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		mGitInitialCommitMessageLabel.setText(LABEL_GIT_INITIAL_COMMIT_MESSAGE);
		
		mGitInitialCommitMessageText = new Text(container, SWT.BORDER);
		mGitInitialCommitMessageText.setText(DEFAULT_GIT_INITIAL_COMMIT_MESSAGE);
		mGitInitialCommitMessageText.setMessage(MESSAGE_GIT_INITIAL_COMMIT_MESSAGE);
		mGitInitialCommitMessageText.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
		GridData gd_gitInitialCommitMessageText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		//gd_gitEmailText.widthHint = 277;
		mGitInitialCommitMessageText.setLayoutData(gd_gitInitialCommitMessageText);
		mGitInitialCommitMessageText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {onDialogChanged();}
		});
		
		updateGitSection();
	}
	
	/** @param apiTargets targets api to set {@link #mApiTargets} */
	private final void setApiTargets(String[] apiTargets) {
		mApiTargets = apiTargets;
		invalidateApiTargets();
		onDialogChanged();
	}
	
	/** Update {@link #mTargetApiCombo} widget when it accessible and {@link #mApiTargets} accessible too. */
	private final void invalidateApiTargets() {
		if (mTargetApiCombo != null && mApiTargets != null) {
			mTargetApiCombo.setItems(mApiTargets);
			mTargetApiCombo.select(mApiTargets.length - 1);
			mTargetApiCombo.setEnabled(true);
		}
	}

	
	/**
	 * Causes after changes in text field.
	 * 
	 * It necessary for validate input data.
	 */
	private final void onDialogChanged() {
		final String fileName = getProjectHandle().getName();
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
		
		final String packageName = mPackageNameText.getText();
		if (packageName.length() == 0) {
			updateStatus("Package name must be specified");
			return;
		}
		
		if (!packageName.matches(REGEXP_PACKAGE_NAME)) {
			updateStatus("Project name must be valid");
			return;
		}
		
		final String repository = mGitRepositoryText.getText();
		if (repository != null && !repository.isEmpty()) {
			
			if (mGitUserText.isVisible()) {
				final String user = mGitUserText.getText();
				if (user == null || user.isEmpty()) {
					updateStatus("User name can not be empty");
					return;
				}
			}
			
			final String password = mGitPassText.getText();
			if (password == null || password.isEmpty()) {
				updateStatus("Password can not be empty");
				return;
			}
			
			final String authorName = mGitAuthorNameText.getText();
			if (authorName == null || authorName.isEmpty()) {
				updateStatus("Author name can not be empty");
				return;
			}
			
			final String authorEmail = mGitAuthorEmailText.getText();
			if (authorEmail != null && !authorEmail.isEmpty()) {
				if (!Pattern.compile(REGEXP_EMAIL)
						.matcher(authorEmail).matches()) {
					updateStatus("Incorrect Author email");
					return;
				}
			} else {
				updateStatus("Author email can not be empty");
				return;
			}
			
			final String branch = mGitInitialBranchText.getText();
			if (branch == null || branch.isEmpty()) {
				updateStatus("Initial branch can not be empty");
				return;
			}
			
			final String message = mGitInitialCommitMessageText.getText();
			if (message == null || message.isEmpty()) {
				updateStatus("Initial commit message can not be empty");
				return;
			}

		}
		
		updateStatus(null);
		
	}

	/** @param message for update wizard status */
	private final void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	/**	Update git controls visibility. */
	private final void updateGitSection() {
		final String gitRepositoryText = mGitRepositoryText.getText();
		final boolean visible = gitRepositoryText != null && gitRepositoryText.length() > 0;
		
		mGitPassLabel.setVisible(visible);
		mGitPassText.setVisible(visible);
		mGitAuthorNameLabel.setVisible(visible);
		mGitAuthorNameText.setVisible(visible);
		mGitAuthorEmailLabel.setVisible(visible);
		mGitAuthorEmailText.setVisible(visible);
		mGitInitialBranchLabel.setVisible(visible);
		mGitInitialBranchText.setVisible(visible);
		mGitInitialCommitMessageLabel.setVisible(visible);
		mGitInitialCommitMessageText.setVisible(visible);
		
		if (visible) {
			if (gitRepositoryText.startsWith("https://")) {
				mGitUserText.setVisible(true);
				mGitUserLabel.setVisible(true);
				mGitPassLabel.setText(LABEL_GIT_USER_PASS_HTTPS);
				if (gitRepositoryText.contains("github.com") ||
							gitRepositoryText.contains("bitbucket.org")) {
					final int indexBB = gitRepositoryText.indexOf("bitbucket.org");
					String path = gitRepositoryText.replace("https://github.com/", "");
					if (indexBB != -1) path = path.substring(indexBB + "bitbucket.org".length() + 1);
					mGitUserText.setText(path.substring(0, path.indexOf("/")));
					mGitUserText.setEnabled(false);
				} else
					mGitUserText.setEnabled(true);
			} else
				mGitPassLabel.setText(LABEL_GIT_USER_PASS_SSH);
			mGitUserLabel.getParent().layout();
		} else {
			mGitUserText.setVisible(false);
			mGitUserLabel.setVisible(false);
		}
		
		onDialogChanged();
	}
	
	/** @return selected package name */
	final String getPackageName() {return mPackageNameText.getText();}
	/** @return selected target api */
	final String getTargetApi() {return mTargetApiCombo.getText();}
	/** @return selected git repository */
	final String getGitRepository() {return mGitRepositoryText.getText();}
	/** @return selected git branch */
	final String getGitBranch() {return mGitInitialBranchText.getText();}
	/** @return selected git author name */
	final String getGitAuthorName() {return mGitAuthorNameText.getText();}
	/** @return selected git author email */
	final String getGitAuthorEmail() {return mGitAuthorEmailText.getText();}
	/** @return selected git user name */
	final String getGitUserName() {return mGitUserText.getText();}
	/** @return selected git user password */
	final String getGitUserPassword() {return mGitPassText.getText();}
	/** @return selected git commit message */
	final String getGitCommitMessage() {return mGitInitialCommitMessageText.getText();}
	

	
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
			while ((line = bufferedReader.readLine()) != null)
				if (line.startsWith("id: "))
					targets.add(line.substring(line.indexOf("\"") + 1,
							line.length() - 1).replace(" (x86 System Image)", ""));
			result = targets.toArray(new String[targets.size()]);
		    bufferedReader.close();
		} catch (IOException | InterruptedException e) {e.printStackTrace();}
		return result;
	}

	
	/**
	 * @author Nikitenko Gleb
	 */
	interface WizardCallback {

	}


	/**
	 * Task for search installed android targets in system.
	 * 
	 * @author Nikitenko Gleb
	 */
	private final class DetectInstalledTargets implements Runnable {
		
		/**	Return targets callback. */
		private final ReturnInstalledTargets mReturnInstalledTargets;
	
		/**
		 * Constructs new task with return targets callback.
		 * 
		 * @param returnInstalledTargets the return targets callback
		 */
		public DetectInstalledTargets(ReturnInstalledTargets returnInstalledTargets) {
			mReturnInstalledTargets = returnInstalledTargets;
		}
	
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public final void run() {
			final String[] targetApis = getApiTargets();
			if (targetApis == null || targetApis.length == 0) return;
			mReturnInstalledTargets.returnDetectedApis(targetApis);
		}
	
	}
	
	/**
	 * Task for return installed android targets in system to UI-Thread.
	 * 
	 * @author Nikitenko Gleb
	 */
	private final class ReturnInstalledTargets implements Runnable {
		
		/**	Weak reference to wizard page. */
		private final WeakReference<WizardPage> mWizardPage;
		
		/**	Detected api targets in system. */
		private String[] mApiTargets;
	
		/**
		 * Constructs new task with wizard page reference.
		 * 
		 * @param wizardPage target wizard page for update ui after take target apis.
		 * @param apiTargets {@link #mApiTargets}
		 */
		public ReturnInstalledTargets(WizardPage wizardPage) {
			mWizardPage = new WeakReference<WizardPage>(wizardPage);
		}
		
		/** Send to ui-thread detected apis */
		public final void returnDetectedApis(String[] apiTargets) {
			mApiTargets = apiTargets;
			final WizardPage wizardPage = mWizardPage.get();
			if (wizardPage == null) return;
			final Shell shell = wizardPage.getShell();  
			if (shell == null) return;
			final Display display = shell.getDisplay();
			if (display == null) return;
			display.asyncExec(this);
		}
	
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public final void run() {
			final WizardPage wizardPage = mWizardPage.get();
			if (wizardPage == null) return;
			wizardPage.setApiTargets(mApiTargets);
		}
	
	}

	


}
