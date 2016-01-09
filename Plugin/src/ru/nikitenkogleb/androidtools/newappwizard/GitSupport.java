/** GitSupport.java */
package ru.nikitenkogleb.androidtools.newappwizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.runtime.Path;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Git features.
 * 
 * @author Nikitenko Gleb
 */
@SuppressWarnings("restriction")
final class GitSupport {
	
	/** HTTPS credentials */
	private final UsernamePasswordCredentialsProvider mHttpsCredentialsProvider;
	/**	SSH credentials */
	private final SSHConfigCallback mSshConfigCallback;
	/**	The project path */
	private String mProjectPath = null;
	
	/**	Creates new Git Support module */
	public GitSupport(String login, String password, String repository,
			String projectPath, String tempFolder, String initBranch) {
		
		final String home = System.getProperty("user.home");
		if (login == null || login.isEmpty()) {
			mSshConfigCallback = new SSHConfigCallback(home + Path.SEPARATOR +
					".ssh" + Path.SEPARATOR + "id_rsa",
					new CredentialsProvider(password));
			mHttpsCredentialsProvider = null;
		} else {
			mSshConfigCallback = null;
			mHttpsCredentialsProvider =
					new UsernamePasswordCredentialsProvider(login, password);
		}
		
		try {
			final CloneCommand cloneCommand = Git.cloneRepository()
					.setURI(repository).setDirectory(new File(tempFolder));
			
			if (mSshConfigCallback != null)
				cloneCommand.setTransportConfigCallback(mSshConfigCallback);
			else
				cloneCommand.setCredentialsProvider(mHttpsCredentialsProvider);
			
			final Git mGit = cloneCommand.call();
			mGit.checkout().setCreateBranch(true).setName(initBranch).call();
			mGit.close();
			
			move(new File(tempFolder + "/.git"), new File(projectPath + "/.git"));
			move(new File(tempFolder + "/README.md"), new File(projectPath + "/README.md"));
			move(new File(tempFolder + "/LICENSE"), new File(projectPath + "/LICENSE"));
			move(new File(tempFolder + "/.gitignore"), new File(projectPath + "/.gitignore"));
			
			new File(tempFolder).delete();
			mProjectPath = projectPath;
			
		} catch (GitAPIException | IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Move directory.
	 * 
	 * @param src	source directory
	 * @param dst	destination directory
	 */
	private final void move(File src, File dst) throws IOException {
		if (src.isDirectory()) {
			if (!dst.exists()) dst.mkdir();
			final String[] childrens = src.list();
			for (int i = 0; i < childrens.length; i++) move(new File(src, childrens[i]), new File(dst, childrens[i]));
		} else {
			final InputStream in = new FileInputStream(src);
            final OutputStream out = new FileOutputStream(dst);
            final byte[] buf = new byte[8192]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
		}
        src.delete();
	}
	
	/** @return true if clone was successful */
	final boolean isSuccessful() {return mProjectPath != null && !mProjectPath.isEmpty();}
	
	/**	Send changes to remote repository */
	final void close(String userName, String userEmail, String initMessage) {
		if (!isSuccessful()) return;
		FileRepository mRepository = null;
		Git mGit = null;
		try {
			
			mRepository = new FileRepository(new File(mProjectPath, ".git"));
			mGit = new Git(mRepository);
			
			mGit.add().addFilepattern(".").call();
			mGit.commit().setAll(true).setAuthor(userName, userEmail).setCommitter(userName, userEmail)
			.setMessage(initMessage).call();
			final PushCommand pushCommand = mGit.push()
					.setRemote("origin").setPushAll();
			
			if (mSshConfigCallback != null)
				pushCommand.setTransportConfigCallback(mSshConfigCallback);
			else
				pushCommand.setCredentialsProvider(mHttpsCredentialsProvider);
			
			pushCommand.call();
		} catch (GitAPIException | IOException e) {e.printStackTrace();}
		
		if (mGit != null) mGit.close();
		if (mRepository != null) mRepository.close();
	}

	/**
	 * Credentials Provider.
	 * 
	 * @author Nikitenko Gleb
	 */
	private final class CredentialsProvider extends org.eclipse.jgit.transport.CredentialsProvider {
	
		/**	SSH Passphrase. */
		private final String mPassphrase;
		
		/** Creates new credential provider	 */
		public CredentialsProvider(String passphrase) {
			mPassphrase = passphrase;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jgit.transport.CredentialsProvider
		 * #get(org.eclipse.jgit.transport.URIish, org.eclipse.jgit.transport.CredentialItem[])
		 */
		@Override
		public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
			for (int i = 0; i < items.length; i++)
				((CredentialItem.StringType) items[i]).setValue(mPassphrase);
            return true;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jgit.transport.CredentialsProvider#isInteractive()
		 */
		@Override
		public boolean isInteractive() {return false;}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jgit.transport.CredentialsProvider
		 * #supports(org.eclipse.jgit.transport.CredentialItem[])
		 */
		@Override
		public boolean supports(CredentialItem... items) {return true;}
	
	}

	/**
	 * Custom Config Session Factory.
	 * 
	 * @author Nikitenko Gleb
	 */
	private class SSHConfigSessionFactory extends JschConfigSessionFactory {
		
		/**	Path to private ssh key. */
		private final String mPrivateKeyPath;
		/**	Credentials Provider	 */
		private final CredentialsProvider mCredentialsProvider;

		/**	Creates new ssh config session factory. */
		SSHConfigSessionFactory(String keyPath,
				CredentialsProvider credentialsProvider) {
			mPrivateKeyPath = keyPath;
			mCredentialsProvider = credentialsProvider;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jgit.transport.JschConfigSessionFactory
		 * #configure(org.eclipse.jgit.transport.OpenSshConfig.Host, com.jcraft.jsch.Session)
		 */
		@Override
		protected final void configure(Host host, Session session) {
			session.setUserInfo(new CredentialsProviderUserInfo(session, mCredentialsProvider));
			//session.setPassword(mPassword);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jgit.transport.JschConfigSessionFactory
		 * #createDefaultJSch(org.eclipse.jgit.util.FS)
		 */
		@Override
		protected final JSch createDefaultJSch(FS fs) throws JSchException {
			final JSch defaultJSch = super.createDefaultJSch(fs);
			defaultJSch.removeAllIdentity();
			defaultJSch.addIdentity(mPrivateKeyPath);
			return defaultJSch;
		}

	}

	/**
	 * Custom SSH Config Transport
	 * 
	 * @author Nikitenko Gleb
	 */
	private final class SSHConfigCallback implements TransportConfigCallback {
		
		/**	SSH Session Factory. */
		private final SSHConfigSessionFactory mSessionFactory;
		
		/**
		 * Creates new SSH Config callback.
		 * 
		 * @param keyPath path to private ssh key.
		 * @param credentialsProvider credentials provider
		 */
		SSHConfigCallback(String keyPath, CredentialsProvider credentialsProvider) {
			mSessionFactory = new SSHConfigSessionFactory(keyPath, credentialsProvider);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jgit.api.TransportConfigCallback
		 * #configure(org.eclipse.jgit.transport.Transport)
		 */
		@Override
		public final void configure(Transport transport) {
			((SshTransport)transport).setSshSessionFactory(mSessionFactory);
		}
	}
}
