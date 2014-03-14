package ch.bergturbenthal.wisp.manager.service.provision.routeros;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.experimental.Builder;

@Data
@Builder
public class PrintLine {
	public static PrintLine parseLine(final int numberLength, final int flagLength, final String line) {
		if (line.length() < numberLength + flagLength) {
			return null;
		}
		final int lineNumber = Integer.parseInt(line.substring(0, numberLength).trim());
		final Set<Character> flags = new LinkedHashSet<>();
		final CharSequence flagString = line.subSequence(numberLength, flagLength + numberLength);
		for (int i = 0; i < flagString.length(); i++) {
			final char flagChar = flagString.charAt(i);
			if (Character.isWhitespace(flagChar)) {
				continue;
			}
			flags.add(Character.valueOf(flagChar));
		}
		final Map<String, String> parsedValues = new LinkedHashMap<>();
		final int lastSpace = numberLength + flagLength;
		String currentKey = null;
		boolean inString = false;
		final StringBuilder currentWord = new StringBuilder();
		final StringBuilder currentValue = new StringBuilder();
		for (int i = numberLength + flagLength; i < line.length(); i++) {
			final char currentChar = line.charAt(i);
			if (inString) {
				if (currentChar == '"') {
					inString = !inString;
				} else {
					currentWord.append(currentChar);
				}
			} else {
				if (currentChar == '"') {
					inString = !inString;
					continue;
				}
				if (currentChar == '=') {
					if (currentKey != null) {
						parsedValues.put(currentKey, currentValue.subSequence(0, currentValue.length() - 1).toString());
						currentValue.setLength(0);
					}
					currentKey = currentWord.toString();
					currentWord.setLength(0);
					continue;
				}
				if (currentChar == ' ') {
					currentValue.append(currentWord);
					currentValue.append(' ');
					currentWord.setLength(0);
					continue;
				}
				currentWord.append(currentChar);
			}
		}
		if (currentKey != null) {
			parsedValues.put(currentKey, currentValue.subSequence(0, currentValue.length() - 1).toString());
			currentValue.setLength(0);
		}
		// final String[] values = Pattern.compile(" ").split(line.subSequence(numberLength + flagLength, line.length()));
		// for (final String value : values) {
		// final int separatorChar = value.indexOf('=');
		// if (separatorChar < 0) {
		// parsedValues.put(value, "");
		// continue;
		// }
		// parsedValues.put(value.substring(0, separatorChar), value.substring(separatorChar + 1));
		// }
		return PrintLine.builder().number(lineNumber).flags(flags).values(parsedValues).build();
	}

	private Set<Character> flags;
	private int number;
	private Map<String, String> values;
}
