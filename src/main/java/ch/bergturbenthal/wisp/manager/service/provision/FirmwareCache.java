package ch.bergturbenthal.wisp.manager.service.provision;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

@Component
public class FirmwareCache {
	private final File cacheBaseDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "fw-cache");
	private final ConcurrentMap<URL, Object> downloadLock = new ConcurrentHashMap<>();

	public File getCacheEntry(final URL downloadUrl) throws IOException {
		downloadLock.putIfAbsent(downloadUrl, new Object());
		final Object lock = downloadLock.get(downloadUrl);
		synchronized (lock) {
			final File resolvedFile = resolveFile(downloadUrl);
			if (resolvedFile.exists()) {
				return resolvedFile;
			}
			if (!resolvedFile.getParentFile().exists()) {
				resolvedFile.getParentFile().mkdirs();
			}
			final File tempFile = new File(cacheBaseDir, UUID.randomUUID().toString());
			final InputStream inputStream = downloadUrl.openStream();
			final FileOutputStream outputStream = new FileOutputStream(tempFile);
			try {
				IOUtils.copyLarge(inputStream, outputStream);
			} finally {
				outputStream.close();
			}
			tempFile.renameTo(resolvedFile);
			return resolvedFile;
		}
	}

	private File resolveFile(final URL downloadUrl) {
		return new File(new File(cacheBaseDir, downloadUrl.getHost()), downloadUrl.getFile());
	}
}
