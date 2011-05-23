package org.tomdroid.sync.WebDAV;

import it.could.util.http.WebDavClient;
import it.could.util.location.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.PreferencesActivity;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class WebDAVSyncService extends SyncService
{

	WebDavClient webDAV;
	// used to get files from WebDAV
	File listFile;
	String actualFileListParted[];

	private static String m_username;
	private static String m_password;
	private static String m_path;

	// Username storage fields
	public static final String USERNAME_STORAGE = "username_storage";
	public static final String USERNAME_FIELD = "username_field";
	public static final String PASSW_STORAGE = "passw_storage";
	public static final String PASSW_FIELD = "passw_field";
	public static final String PATH_STORAGE = "path_storage";
	public static final String PATH_FIELD = "path_field";

	private Activity act;

	/**
	 * Defaul Constructor
	 * 
	 * @param activity
	 *           the given activity
	 * @param handler
	 */
	public WebDAVSyncService(Activity activity, Handler handler)
	{
		super(activity, handler);
		act = activity;

	}

	/**
	 * get the file from the Webdav Server
	 * 
	 * @param fileName
	 */
	public void saveFile(String fileName)
	{
		File file = new File(Tomdroid.NOTES_PATH, fileName);
		if (!file.exists())
		{
			file.delete();
		}
		try
		{
			InputStream in = webDAV.get(fileName);
			byte[] buffer = new byte[2056];
			int len = in.read(buffer);

			FileOutputStream f = new FileOutputStream(file);
			f.write(buffer, 0, len);
			f.flush();
			f.close();
		} catch (NullPointerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * saveFileList txt with the notes which are currently on the webDAV server
	 */
	public void saveFileList()
	{
		String got = "";
		try
		{
			InputStream in = webDAV.get("test.txt");
			byte[] buffer = new byte[2056]; // increment length
			int len = in.read(buffer);
			got = new String(buffer, 0, len);

			// save fileList file to sd
			listFile = new File(Tomdroid.NOTES_PATH, "test.txt");
			FileOutputStream fos = new FileOutputStream(listFile);
			fos.write(buffer, 0, len);
			fos.flush();
			fos.close();

		} catch (NullPointerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		actualFileListParted = got.split(";");
		for (int i = 0; i < actualFileListParted.length; i++)
		{
			saveFile(actualFileListParted[i]);
		}

	}

	/**
	 * If a file is deleted on the server, recognize the missing file by the
	 * difference in the fileList and delete the file on the server.
	 * 
	 * die fileliste auf dem telefon speichern und dann mit der aktuellen
	 * vergleichen.
	 */
	public void deleteFile()
	{

	}

	/**
	 * 
	 * @return the Username saved into the Storage of the android system
	 */
	public String getUsernameStorage()
	{
		SharedPreferences profileStored = act.getSharedPreferences(USERNAME_STORAGE, 0);

		try
		{
			return profileStored.getString(USERNAME_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
		}
	}

	/**
	 * 
	 * @return the password saved into the Storage of the android system
	 */
	public String getPasswordStorage()
	{
		SharedPreferences profileStored = act.getSharedPreferences(PASSW_STORAGE, 0);

		try
		{
			return profileStored.getString(PASSW_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
		}
	}

	/**
	 * 
	 * @return the Path to the WebDAV server saved into the Storage of the
	 *         android system
	 */
	public String getPathStorage()
	{
		SharedPreferences profileStored = act.getSharedPreferences(PATH_STORAGE, 0);

		try
		{
			return profileStored.getString(PATH_FIELD, "");

		} catch (Exception _ex)
		{
			return "";
		}
	}

	/**
	 * the sync mehtod is executed when the sync button is pressed
	 */
	@Override
	protected void sync()
	{
		setSyncProgress(0);

		SharedPreferences usernameStored = act.getSharedPreferences(USERNAME_STORAGE, 0);
		SharedPreferences.Editor usernameEditor = usernameStored.edit();

		try
		{
			if (m_username != null)
			{
				usernameEditor.putString(USERNAME_FIELD, m_username);
				usernameEditor.commit();
			}
		} catch (Exception _ex)
		{
			_ex.printStackTrace();
		}

		// password save
		SharedPreferences passwordStored = act.getSharedPreferences(PASSW_STORAGE, 0);
		SharedPreferences.Editor passwordEditor = passwordStored.edit();

		try
		{
			if (m_password != null)
			{
				passwordEditor.putString(PASSW_FIELD, m_password);
				passwordEditor.commit();
			}
		} catch (Exception _ex)
		{
			_ex.printStackTrace();
		}

		// path save
		SharedPreferences pathStored = act.getSharedPreferences(PATH_STORAGE, 0);
		SharedPreferences.Editor pathEditor = pathStored.edit();
		try
		{
			if (m_path != null)
			{
				pathEditor.putString(PATH_FIELD, m_path);
				pathEditor.commit();
			}
		} catch (Exception _ex)
		{
			_ex.printStackTrace();
		}

		try
		{
			// WebDavClient(Location.parse("http://PatrickWebDAV:patrickwebdav@193.170.124.44/webdav2/PatrickWebDAV"));
			String username = getUsernameStorage();
			String password = getPasswordStorage();
			String path = getPathStorage();
			String fullPath = "http://" + username + ":" + password + "@" + path;
			webDAV = new WebDavClient(Location.parse(fullPath));

			// in the test.txt the notes on the server are descriped
			saveFileList();
			setSyncProgress(100);

			SyncManager s = new SyncManager();
			s.getSdCardSyncService().startSynchronization();

		} catch (NullPointerException e)
		{
			Toast.makeText(Tomdroid.getInstance(), "Synchronization with " + SyncManager.getInstance().getCurrentService().getDescription() + " failed.", Toast.LENGTH_SHORT).show();
			setSyncProgress(100);
			e.printStackTrace();
		} catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean needsServer()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean needsAuth()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return "WebDAV";
	}

	@Override
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return "WebDAV";
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return m_username;
	}

	/**
	 * @param username
	 *           the username to set
	 */
	public static void setUsername(String username)
	{
		m_username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return m_password;
	}

	/**
	 * @param password
	 *           the password to set
	 */
	public static void setPassword(String password)
	{
		m_password = password;
	}

	/**
	 * @return the password
	 */
	public String getPath()
	{
		return m_path;
	}

	/**
	 * @param password
	 *           the password to set
	 */
	public static void setPath(String path)
	{
		m_path = path;
	}

}
