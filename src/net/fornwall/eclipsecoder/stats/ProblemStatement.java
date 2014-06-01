package net.fornwall.eclipsecoder.stats;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import net.fornwall.eclipsecoder.util.Utilities;

/**
 * A problem statement.
 */
public class ProblemStatement {

	public static final String XML_PREFS_KEY = "problemStatementXml";

	public static class TestCase {
		public Object[] parameters;

		public Object returnValue;

		public TestCase() {
			// no-arg constructor needed by serialization
		}

		public TestCase(Object returnValue, Object[] parameters) {
			this.returnValue = returnValue;
			this.parameters = parameters;
		}

		/** Needed by serialization. */
		public Object[] getParameters() {
			return parameters;
		}

		/** Needed by serialization. */
		public Object getReturnValue() {
			return returnValue;
		}

		/** Needed by serialization. */
		public void setParameters(Object[] parameters) {
			this.parameters = parameters;
		}

		/** Needed by serialization. */
		public void setReturnValue(Object returnValue) {
			this.returnValue = returnValue;
		}
	}

	public static Object parsePrimitiveType(Class<?> c, String valueString) {
		valueString = valueString.trim();
		if (c == Integer.class) {
			return Integer.parseInt(valueString);
		} else if (c == Character.class) {
			return valueString.charAt(1);
		} else if (c == Long.class) {
			return Long.parseLong(valueString);
		} else if (c == Double.class) {
			return Double.parseDouble(valueString);
		} else if (c == String.class) {
			return valueString.substring(1, valueString.length() - 1);
		} else if (c == Boolean.class) {
			return Boolean.parseBoolean(valueString);
		} else {
			throw new IllegalArgumentException("Unknown type: " + c);
		}
	}

	/**
	 * Parse the values contained in text into an instance of the supplied class.
	 * 
	 * @param c
	 *            The class which should be instantiated.
	 * @param text
	 *            The text containing a textual description of the object.
	 * @return An instance of c which has been parsed from text.
	 */
	public static Object parseType(Class<?> c, String text) {
		if (!c.isArray()) {
			return parsePrimitiveType(c, text);
		}

		// remove trailing '{' and '}'
		text = text.trim();
		text = text.substring(1, text.length() - 1).trim();
		if (text.length() == 0)
			return Array.newInstance(c.getComponentType(), 0);

		String[] parts = null;
		if (c.getComponentType() == String.class) {
			// Handle strings with , in body as in "that is, some more"
			List<String> partList = new ArrayList<String>();
			int lastIndex = -1;
			for (int i = 0; i < text.length(); i++) {
				if (text.charAt(i) == '"') {
					if (lastIndex == -1) {
						lastIndex = i;
					} else {
						partList.add(text.substring(lastIndex, i + 1));
						lastIndex = -1;
					}
				}
			}
			parts = partList.toArray(new String[partList.size()]);
		} else {
			parts = text.split(",");
		}

		Object[] result = (Object[]) Array.newInstance(c.getComponentType(), parts.length);
		for (int i = 0; i < result.length; i++)
			result[i] = parsePrimitiveType(c.getComponentType(), parts[i]);

		return result;
	}

	private String contestName;

	private String className;

	private String htmlDescription;

	// if we are in contest and can submit
	boolean inContest = true;

	private String methodName;

	private List<String> parameterNames = new ArrayList<String>();

	private List<Class<?>> parameterTypes = new ArrayList<Class<?>>();

	// String, Integer, Integer[]...
	private Class<?> returnType;

	private List<TestCase> testCases = new ArrayList<TestCase>();

	public String getHtmlDescription() {
		return htmlDescription;
	}

	public List<String> getParameterNames() {
		return parameterNames;
	}

	public List<Class<?>> getParameterTypes() {
		return parameterTypes;
	}

	public Class<?> getReturnType() {
		return returnType;
	}

	public String getSolutionClassName() {
		return className;
	}

	public String getSolutionMethodName() {
		return methodName;
	}

	public List<TestCase> getTestCases() {
		return testCases;
	}

	public boolean isInContest() {
		return inContest;
	}

	public void setHtmlDescription(String htmlDescription) {
		this.htmlDescription = htmlDescription;
	}

	public void setInContest(boolean inContest) {
		this.inContest = inContest;
	}

	public void setParameterNames(List<String> parameterNames) {
		this.parameterNames = new ArrayList<String>(parameterNames);
	}

	public void setParameterTypes(List<Class<?>> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public void setReturnType(Class<?> returnType) {
		this.returnType = returnType;
	}

	public void setSolutionClassName(String className) {
		this.className = className;
	}

	public void setSolutionMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setTestCases(List<TestCase> testCases) {
		this.testCases = testCases;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public String toString() {
		return "CLASS: " + getSolutionClassName() + "\n" + "METHOD: " + getSolutionMethodName() + "\n"
				+ "RETURNVALUE: " + getReturnType() + "\n" + "PARAMETERS: " + getParameterTypes() + "\n";
	}

	/**
	 * Serialize this bean to XML and return the string in utf-8 encoding.
	 * 
	 * @see #fromXML(String)
	 */
	public String toXML() {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		XMLEncoder encoder = new XMLEncoder(buffer);
		encoder.writeObject(this);
		encoder.close();
		try {
			return buffer.toString("utf-8");
		} catch (UnsupportedEncodingException e) {
			// will never happen - utf-8 always supported
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decode xml created by {@link #toXML()}.
	 */
	public static ProblemStatement fromXML(String xml) {
		XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(Utilities.getBytes(xml)));
		ProblemStatement result = (ProblemStatement) decoder.readObject();
		decoder.close();
		return result;
	}

	public String getContestName() {
		return contestName;
	}

	public void setContestName(String contestName) {
		this.contestName = contestName;
	}

}
