package org.eclipse.cdt.internal.errorparsers;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.core.resources.IFile;

public class GCCErrorParser implements IErrorParser {

	public boolean processLine(String line, ErrorParserManager eoParser) {
		// Known patterns.
		// (a)
		// filename:lineno: description
		//
		// (b)
		// filename:lineno:column: description
		//
		// (b)
		// In file included from b.h:2,
		//				 from a.h:3,
		//				 from hello.c:3:
		// c.h:2:15: missing ')' in macro parameter list
		//
		// (c)
		// h.c: In function `main':
		// h.c:41: `foo' undeclared (first use in this function)
		// h.c:41: (Each undeclared identifier is reported only once
		// h.c:41: for each function it appears in.)
		// h.c:41: parse error before `char'
		// h.c:75: `p' undeclared (first use in this function)

		int firstColon = line.indexOf(':');

		/* Guard against drive in Windows platform.  */
		if (firstColon == 1) {
			try {
				String os = System.getProperty("os.name");
				if (os != null && os.startsWith("Win")) {
					try {
						if (Character.isLetter(line.charAt(0))) {
							firstColon = line.indexOf(':', 2);
						}
					} catch (StringIndexOutOfBoundsException e) {
					}
				}
			} catch (SecurityException e) {
			}
		}

		if (firstColon != -1) {
			try {
				int secondColon= line.indexOf(':', firstColon + 1);
				if (secondColon != -1) {
					String fileName = line.substring(0, firstColon);
					String lineNumber = line.substring(firstColon + 1, secondColon);
					String varName = null;
					String desc = line.substring(secondColon + 1).trim();
					int severity = IMarkerGenerator.SEVERITY_ERROR_RESOURCE;
					int	num  = -1;
					int col = -1;

					try {
						num = Integer.parseInt(lineNumber);
					} catch (NumberFormatException e) {
					}

					if (num == -1) {
						// Bail out not recognizable format. i.e. no line numbers
						return false;
					} else {
						/* Then check for the column  */
						int thirdColon= line.indexOf(':', secondColon + 1);
						if (thirdColon != -1) {
							String columnNumber = line.substring(secondColon + 1, thirdColon);
							try {
								col = Integer.parseInt(columnNumber);
							} catch (NumberFormatException e) {
							}
						}
						if (col != -1) {
							desc = line.substring(thirdColon + 1).trim();
						}
					}

					// gnu c: filename:no: (Each undeclared identifier is reported
					// only once. filename:no: for each function it appears in.)
					if (desc.startsWith ("(Each undeclared")) {
						// Do nothing.
						return false;
					} else  {
						String previous = eoParser.getPreviousLine();
						if (desc.endsWith(")")
							&& previous.indexOf("(Each undeclared") >= 0 ) {
							// Do nothing.
							return false;
						}
					}

					/* See if we can get a var name
					 * Look for:
					 * 'foo' undeclared
					 * 'foo' defined but not used
					 * conflicting types for 'foo'
					 *
					 */ 
					 int s;
					 if((s = desc.indexOf("\' undeclared")) != -1) {
					 	int p = desc.indexOf("`");
					 	if (p != -1) {
					 		varName = desc.substring(p+1, s);
					 		//System.out.println("undex varName "+ varName);
					 	}
					 } else if((s = desc.indexOf("\' defined but not used")) != -1) {
					 	int p = desc.indexOf("`");
					 	if (p != -1) {
					 		varName = desc.substring(p+1, s);
					 		//System.out.println("unused varName "+ varName);
					 	}
					 } else if((s = desc.indexOf("conflicting types for `")) != -1) {
					 	int p = desc.indexOf("\'", s);
					 	if (p != -1) {
					 		varName = desc.substring(desc.indexOf("`") + 1, p);
					 		//System.out.println("confl varName "+ varName);
					 	}
					 } else if((s = desc.indexOf("previous declaration of `")) != -1) {
					 	int p = desc.indexOf("\'", s);
					 	if (p != -1) {
					 		varName = desc.substring(desc.indexOf("`") + 1, p);
					 		//System.out.println("prev varName "+ varName);
					 	}
					 } else if ((s = desc.indexOf("parse error before ")) != -1) {
						int p = desc.indexOf("\'", s);
						if (p != -1) {
							varName = desc.substring(desc.indexOf("`") + 1, p);
							//System.out.println("prev varName "+ varName);
						}
					 }

					if (eoParser.getScratchBuffer().startsWith("In file included from ")) {
						if (line.startsWith("from ")) {
							eoParser.appendToScratchBuffer(line);
							return false;
						}
						String buffer = eoParser.getScratchBuffer();
						eoParser.clearScratchBuffer();
						int from = -1;
						while ((from = buffer.lastIndexOf("from ")) != -1) {
							String buf = buffer.substring(from + 5);
							buffer = buffer.substring(0, from);
							if (buf.endsWith(",")) {
								int coma = buf.lastIndexOf(',');
								StringBuffer b = new StringBuffer(buf);
								b.setCharAt(coma, ':');
								b.append(' ').append(buffer).append(" from ").append(line);
								buf = b.toString();
							} else {
								buf = buf + ' ' + buffer + " from " + line;
							}
							processLine(buf, eoParser);
						}
					}

					IFile file = eoParser.findFilePath(fileName);

					if (file == null) {
						// Parse the entire project.
						file = eoParser.findFileName(fileName);
						if (file != null) {
							// If there is a conflict set the error on the project.
							if (eoParser.isConflictingName(fileName)) {
								desc = "*" + desc;
								file = null;
							}
						}
					}
					
					if (desc.startsWith("warning") || desc.startsWith("Warning")) {
						severity = IMarkerGenerator.SEVERITY_WARNING;
						// Remove the warning.
						String d = desc.substring("warning".length()).trim();
						if (d.startsWith(":")) {
							d = d.substring(1).trim();
						}

						if (d.length() != 0) {
							desc = d;
						}
					}
					
					// Display the fileName.
					if (file == null) {
						desc = desc +"[" + fileName + "]";
					}
					eoParser.generateMarker(file, num, desc, severity, varName);
				} else {
					if (line.startsWith("In file included from ")) {
						eoParser.appendToScratchBuffer(line);
					} else if (line.startsWith("from ")) {
						eoParser.appendToScratchBuffer(line);
					}
				}
			} catch (StringIndexOutOfBoundsException e) {
			} catch (NumberFormatException e) {
			}
		}
		return false;
	}
}
