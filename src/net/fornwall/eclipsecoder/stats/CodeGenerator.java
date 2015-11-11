package net.fornwall.eclipsecoder.stats;

import java.util.regex.Pattern;

/**
 * Abstract base class for code generators for different programming languages.
 * 
 * A code generator should be able to map java language constructs to that of another programming language.
 * 
 * Note that this class is just for code generation - support for eclipse integration is from the appropriate language
 * support.
 */
public abstract class CodeGenerator {

	public static final String TAG_CLASSNAME = "$CLASSNAME$";
	public static final String TAG_DUMMYRETURN = "$DUMMYRETURN$";
	public static final String TAG_METHODNAME = "$METHODNAME$";
	public static final String TAG_METHODPARAMS = "$METHODPARAMS$";
	public static final String TAG_RETURNTYPE = "$RETURNTYPE$";
	public static final String TAG_MODULO = "$MODULO$";

	protected ProblemStatement problemStatement;

	public CodeGenerator(ProblemStatement problemStatement) {
		this.problemStatement = problemStatement;
	}

	/**
	 * Method used for code generation of the solution stub. Should return a dummy value so that the initial code stub
	 * returned by getSolutionStub() compiles.
	 * 
	 * The $DUMMYRETURN$ tag will be replaced with this value.
	 * 
	 * @return A dummy return value.
	 */
	public abstract String getDummyReturnString();

	/**
	 * Should return the replacement for the $METHODPARAMS$ tag. Utility method for use in getSolutionStub().
	 */
	protected String getMethodParamsString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < problemStatement.getParameterNames().size(); i++) {
			if (i != 0) {
				builder.append(", ");
			}
			String typeString = getTypeString(problemStatement.getParameterTypes().get(i));
			if (typeString.length() > 0) {
				builder.append(typeString);
				builder.append(' ');
			}
			builder.append(problemStatement.getParameterNames().get(i));
		}
		return builder.toString();
	}

	/**
	 * Get the code template for the language. The code template should contain the relevant tags which will be
	 * substituted.
	 * 
	 * <p>
	 * The code template should be settable from a preference page and should default to a reasonable value.
	 * 
	 * <p>
	 * This method is not intended to be overridden by subclasses unless there is special need.
	 * 
	 * @return the code template with the variables replaced
	 */
	public String getSolutionStub(String codeTemplate) {
		return codeTemplate.replaceAll(Pattern.quote(TAG_CLASSNAME), problemStatement.getSolutionClassName())
				.replaceAll(Pattern.quote(TAG_METHODNAME), problemStatement.getSolutionMethodName())
				.replaceAll(Pattern.quote(TAG_METHODPARAMS), getMethodParamsString())
				.replaceAll(Pattern.quote(TAG_DUMMYRETURN), getDummyReturnString())
				.replaceAll(Pattern.quote(TAG_RETURNTYPE), getTypeString(problemStatement.getReturnType()))
				.replaceAll(Pattern.quote(TAG_MODULO), getModuloString());
	}

	/**
	 * Should return the source for a test suite file which tests the solution.
	 */
	public abstract String getTestsSource();

	/**
	 * Map a java language class to the matching language type.
	 * 
	 * Implementations for scripting languages without explicit types should return an empty string.
	 * 
	 * @param type
	 *            The java class to map.
	 * @return The matching language type.
	 */
	public abstract String getTypeString(Class<?> type);
	
	/**
	 * Returns the modulo string, appropriate for the language.
	 * 
	 * @return modulo string, or empty string if none or not implemented
	 */
	public String getModuloString() {
		return "";
	}
}
