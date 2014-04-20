package ch.bergturbenthal.wisp.manager.service.provision;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.Cleanup;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

@Component
public class FirmwareCache {

	public static interface UrlStreamProducer {
		InputStream createInputStream(final URL url) throws IOException;
	}

	private final File cacheBaseDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "fw-cache");
	private final ConcurrentMap<URL, Object> downloadLock = new ConcurrentHashMap<>();

	public File getCacheEntry(final URL downloadUrl) throws IOException {
		return getCacheEntry(downloadUrl, new UrlStreamProducer() {

			@Override
			public InputStream createInputStream(final URL url) throws IOException {
				return url.openStream();
			}
		});
	}

	public File getCacheEntry(final URL downloadUrl, final UrlStreamProducer streamProducer) throws IOException {
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
			@Cleanup
			final InputStream inputStream = streamProducer.createInputStream(downloadUrl);
			@Cleanup
			final FileOutputStream outputStream = new FileOutputStream(tempFile);
			IOUtils.copyLarge(inputStream, outputStream);
			tempFile.renameTo(resolvedFile);
			return resolvedFile;
		}
	}

	private File resolveFile(final URL downloadUrl) {
		return new File(new File(cacheBaseDir, downloadUrl.getHost()), downloadUrl.getFile());
	}
}
