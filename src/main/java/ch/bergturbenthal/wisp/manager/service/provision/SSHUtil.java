package ch.bergturbenthal.wisp.manager.service.provision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Slf4j
public class SSHUtil {
	private static int checkAck(final InputStream in) throws IOException {
		final int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0) {
			return b;
		}
		if (b == -1) {
			return b;
		}

		if (b == 1 || b == 2) {
			final StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				log.error(sb.toString());
			}
			if (b == 2) { // fatal error
				log.error(sb.toString());
			}
		}
		return b;
	}

	public static void copyToDevice(final Session session, final File fromFile, final File toFile) throws JSchException, IOException {
		final ChannelExec channelExec = SSHUtil.createChannelWithCmd(session, "scp -v -t " + toFile.getPath());
		try {
			@Cleanup
			final OutputStream outputStream = channelExec.getOutputStream();
			@Cleanup
			final InputStream inputStream = channelExec.getInputStream();
			channelExec.connect();
			outputStream.write(("C0644 " + fromFile.length() + " " + toFile.getName() + "\n").getBytes());
			outputStream.flush();
			if (checkAck(inputStream) != 0) {
				throw new RuntimeException("Unexpected Error from device while transferring " + fromFile);
			}
			@Cleanup
			final FileInputStream fis = new FileInputStream(fromFile);
			IOUtils.copy(fis, outputStream);
			if (checkAck(inputStream) != 0) {
				throw new RuntimeException("Unexpected Error from device while transferring " + fromFile);
			}
		} finally {
			channelExec.disconnect();
		}
	}

	public static ChannelExec createChannelWithCmd(final Session session, final String cmd) throws JSchException {
		final ChannelExec cmdChannel = (ChannelExec) session.openChannel("exec");
		cmdChannel.setCommand(cmd);
		cmdChannel.setErrStream(System.err);
		return cmdChannel;
	}

	public static ChannelExec sendCmd(final Session session, final String cmd) throws JSchException {
		final ChannelExec cmdChannel = createChannelWithCmd(session, cmd);
		cmdChannel.setInputStream(null);
		cmdChannel.connect();
		return cmdChannel;
	}

	public static void sendCmdWithoutAnswer(final Session session, final String cmd) throws JSchException, IOException {
		final ChannelExec channel = sendCmd(session, cmd);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
		while (true) {
			final String line = reader.readLine();
			if (line == null) {
				break;
			}
			log.info(line);
		}
		channel.disconnect();
	}

}
