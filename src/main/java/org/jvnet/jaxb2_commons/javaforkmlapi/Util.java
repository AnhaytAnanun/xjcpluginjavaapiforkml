package org.jvnet.jaxb2_commons.javaforkmlapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.generator.bean.ClassOutlineImpl;
import com.sun.tools.xjc.generator.bean.field.UntypedListField;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSParticle;

public class Util {
	//private static final Logger LOG = Logger.getLogger(Util.class.getName());
	final static Logger LOG = Logger.getLogger(Util.class.getName());

	
	public static void logInfo(final String output) {
	  LOG.info(XJCJavaForKmlApiPlugin.PLUGINNAME + " " + output);
  }
	
	public static String calculateMethodName(final ClassOutlineImpl cc, final JPackage rootPackage) {
		String tmppackagePrefix = "";
		 if (!cc._package()._package().name().equals(rootPackage.name())) {
		 tmppackagePrefix = cc._package()._package().name().replaceAll("(.*)(\\.)(.*$)", "$3");
		 tmppackagePrefix = upperFirst(tmppackagePrefix);
		 }

		final String methodName = tmppackagePrefix + cc.target.getSqueezedName();
		return methodName;
	}

	public static String eliminateTypeSuffix(String namewithoutType) {
		// namewithoutType = namewithoutType.toLowerCase();
		// if ($currentJavaFile[$i] =~ s/(.*?<)(JAXBElement<. extends )(.*?)(>)(.*?)/$1$3$5/g) {
		namewithoutType = namewithoutType.replaceAll("(.*?)(JAXBElement<. extends )(.*?)(>)(.*?)", "$1$3$5");

		if (namewithoutType.toLowerCase().endsWith("type")) {
			namewithoutType = namewithoutType.substring(0, namewithoutType.length() - 4);
		}
		if (namewithoutType.toLowerCase().endsWith("enum")) {
			namewithoutType = namewithoutType.substring(0, namewithoutType.length() - 4);
		}

		if (namewithoutType.toLowerCase().startsWith("abstract")) {
			if (!namewithoutType.toLowerCase().startsWith("abstractobject") && !namewithoutType.toLowerCase().startsWith("abstractlatlonbox") && !namewithoutType
			    .toLowerCase().startsWith("abstractview")) {
				namewithoutType = namewithoutType.substring(8, namewithoutType.length());
			}
		}

		return namewithoutType;
	}

	/** Change the first character to the lower case. */
	public static String lowerFirst(final String s) {
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	/** Change the first character to the upper case. */
	public static String upperFirst(final String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/**
	 * Returns contents to be added to javadoc.
	 */
	public static final ArrayList<JType> listPossibleTypes(final ClassOutlineImpl outline, final CPropertyInfo prop) {
		final ArrayList<JType> r = new ArrayList<JType>();
		for (final CTypeInfo tt : prop.ref()) {
			r.add(tt.getType().toType(outline.parent(), Aspect.EXPOSED));
		}
		return r;
	}

	public static HashMap<String, ClassOutlineImpl> getClassList(final Outline outline) {
		final HashMap<String, ClassOutlineImpl> classList = new HashMap<String, ClassOutlineImpl>();
		for (final ClassOutline classOutline : outline.getClasses()) {
			final ClassOutlineImpl cc = (ClassOutlineImpl) classOutline;
			classList.put(cc.target.fullName(), cc);
		}
		return classList;
	}

	public static JPackage getKmlClassPackage(final Outline outline) {
		JPackage rootPackage = null;
		for (final PackageOutline packageoutline : outline.getAllPackageContexts()) {
			for (final ClassOutline classOutline : packageoutline.getClasses()) {
				final String currentClassName = Util.eliminateTypeSuffix(classOutline.implRef.name().toLowerCase());
				// LOG.info(">>>>>>>> " + currentClassName);
				if (currentClassName.equals("kml")) {
					//LOG.info(XJCJavaForKmlApiPlugin.PLUGINNAME + " package for kml-class: " + classOutline._package()._package().name() + " set as main package.");
					rootPackage = classOutline._package()._package();
					break;
				}
			}
		}

		return rootPackage;
	}

	public static HashSet<String> getAllNestedClasses(final Outline outline) {
	  final HashSet<String> nestedClasses = new HashSet<String>();
		for (final ClassOutline classOutline : outline.getClasses()) {
			final ClassOutlineImpl cc = (ClassOutlineImpl) classOutline;
			final JClass[] classes = cc.implClass.listClasses();
			for (final JClass jClass : classes) {
	      
//	      LOG.info(">>>> " + jClass.fullName());
	      nestedClasses.add(jClass.fullName());
      }
		}
		return nestedClasses;
  }
	
	public static boolean isRelevantField(final JFieldVar field) {
		final int mods = field.mods().getValue();

		if ((mods & JMod.PRIVATE) != 0) {
			return false;
		}
		if ((mods & JMod.STATIC) != 0) {
			return false;
		}

		if (field.name().contains("Deprecated")) {
			return false;
		}

		if (field.type().name().contains("Deprecated")) {
			return false;
		}

		if (field.name().equals("AbstractObject")) {
			logInfo("skip abstract object");
			return false;
		}
		return true;
	}

	public static Collection<JFieldVar> getSuperclassFields(JCodeModel cm, final ClassOutline classOutline, final boolean collectionsOnly, final boolean checkRequired) {
		final List<JFieldVar> fieldList = new LinkedList<JFieldVar>();
		ClassOutline superclass = classOutline.getSuperClass();
		while (superclass != null) {
			// LOG.info("Retrieving fields for superclass [" + superclass.implClass + "]");
			fieldList.addAll(0, Util.getFields(superclass, collectionsOnly, checkRequired));
			superclass = superclass.getSuperClass();
		}

		// LOG.info("Fields retrieved for superclass of class [" + classOutline.implClass + "] are [" + fieldList + "].");
		for (JFieldVar jFieldVar : fieldList) {
	    jFieldVar.type(Util.removeJAXBElement(cm, jFieldVar.type()));
    }
		return fieldList;
	}

	public static CPropertyInfo searchPropertyInfo(final ClassOutline classOutline, final String name) {
		CPropertyInfo propertyInfo = classOutline.target.getProperty(name);

		if ((propertyInfo == null) && (classOutline.getSuperClass() != null)) {
			propertyInfo = searchPropertyInfo(classOutline.getSuperClass(), name);
		}

		return propertyInfo;
	}

	public static Collection<JFieldVar> getSuperclassAllFields(final ClassOutline classOutline, final boolean collectionsOnly) {
		final List<JFieldVar> fieldList = new LinkedList<JFieldVar>();
		ClassOutline superclass = classOutline.getSuperClass();
		while (superclass != null) {
			// LOG.info("Retrieving fields for superclass [" + superclass.implClass + "]");
			fieldList.addAll(0, getAllFieldsFields(superclass, collectionsOnly));
			superclass = superclass.getSuperClass();
		}

		// LOG.info("Fields retrieved for superclass of class [" + classOutline.implClass + "] are [" + fieldList + "].");

		return fieldList;
	}

	public static JType removeJAXBElement(JCodeModel cm, final JType jFieldVar) {
		
	  final List<JClass> type = jFieldVar.boxify().getTypeParameters();
	  if (type.size() > 0) {
	  	JClass clazz = type.get(0);
	  	if (clazz.fullName().startsWith("javax.xml.bind.JAXBElement<")) {
//	  		LOG.info("!111>>>>>>>>" + clazz.fullName());

	  		final List<JClass> typeParameters = clazz.getTypeParameters();
	  		final JClass clazz2 = typeParameters.get(0);
//	  		LOG.info("!222>>>>>>>>" + clazz2.fullName());
	  		if (clazz2.fullName().startsWith("? extends")) {
	  			clazz = clazz2._extends();
	  			// if (clazz2._extends().fullName().equals("java.lang.Object")) {
//	  			LOG.info("!333a>>>>>>>" + clazz.fullName());
	  		} else {
	  			clazz = clazz2;
//	  			LOG.info("!333b>>>>>>>" + clazz.fullName());
	  		}
	  		if (jFieldVar.fullName().startsWith("java.util.List")) {
	  			JClass list = cm.ref(List.class);
	  			LOG.info(">>>>>>> change " + jFieldVar.name() + " -> " + list.narrow(clazz).name() );
	  			//jFieldVar.type(arrayList.narrow(clazz));
	  			return list.narrow(clazz);
	  		} else if (jFieldVar.fullName().startsWith("java.util.Arrayist")) {
	  			JClass arrayList = cm.ref(ArrayList.class);
	  			LOG.info(">>>>>>> change " + jFieldVar.name() + " -> " + arrayList.narrow(clazz).name() );
	  			//jFieldVar.type(arrayList.narrow(clazz));
	  			return arrayList.narrow(clazz);
	  		} else {
	  			LOG.info(">>>>>>> change " + jFieldVar.name() + " -> " + clazz.name());
	  			//jFieldVar.type(clazz);
	  			return clazz;
	  		}
	  	}
	  }
		return jFieldVar;
  }
	
	public static Collection<JFieldVar> getAllFieldsFields(final ClassOutline classOutline, final boolean collectionsOnly) {
		// LOG.info("Retrieving fields for class [" + classOutline.implClass + "]");
		final Map<String, JFieldVar> fields = classOutline.implClass.fields();
		final Collection<JFieldVar> fieldList = new ArrayList<JFieldVar>();
		for (final FieldOutline fo : classOutline.getDeclaredFields()) {
			if (collectionsOnly) {
				if (!(fo instanceof UntypedListField)) {
					continue;
				}
			}
			
			final JFieldVar var = fields.get(fo.getPropertyInfo().getName(false));
			if (var == null) {
				continue;
			}
			if (!isRelevantField(var)) {
				continue;
			}
			fieldList.add(var);

		}
		// LOG.info("Fields retrieved for class [" + classOutline.implClass + "] are [" + fieldList + "].");
		return fieldList;
	}

	public static Collection<JFieldVar> getFields(final ClassOutline classOutline, final boolean collectionsOnly, final boolean checkRequired) {
		// LOG.info("Retrieving fields for class [" + classOutline.implClass + "]");
		final Map<String, JFieldVar> fields = classOutline.implClass.fields();
		final Collection<JFieldVar> fieldList = new ArrayList<JFieldVar>();
		for (final FieldOutline fo : classOutline.getDeclaredFields()) {
			if (collectionsOnly) {
				if (!(fo instanceof UntypedListField)) {
					continue;
				}
			}
			
			if (checkRequired) {
				if (!isFieldMarkedAsRequiredOrMandatoryInXSDSchema(fo)) {
					continue;
				}
			} else if (!checkRequired) {
				if (isFieldMarkedAsRequiredOrMandatoryInXSDSchema(fo)) {
					continue;
				}
			}
			
			final JFieldVar var = fields.get(fo.getPropertyInfo().getName(false));
			if (var == null) {
				continue;
			}
			if (!isRelevantField(var)) {
				continue;
			}
			fieldList.add(var);
		}
		// LOG.info("Fields retrieved for class [" + classOutline.implClass + "] are [" + fieldList + "].");
		return fieldList;
	}

	// #########################################
	// #########################################
	// #########################################
	// #########################################
	// #########################################
	// #########################################

	public static boolean isFieldMarkedAsRequiredOrMandatoryInXSDSchema(final FieldOutline fo) {
		final CPropertyInfo pi = fo.getPropertyInfo();
		if (pi.getSchemaComponent() instanceof XSParticle) {
			final XSParticle particle = (XSParticle) pi.getSchemaComponent();
			if (particle.getMinOccurs() > 0) {
				// LOG.info("!!! MinOccurs of Element [" + pi.getName(false) + "] is [" + particle.getMinOccurs() + "].");
			}
			return particle.getMinOccurs() > 0;
		} else if (pi.getSchemaComponent() instanceof XSAttributeUse) {
			final XSAttributeUse attributeUse = (XSAttributeUse) pi.getSchemaComponent();
			if (attributeUse.isRequired()) {
				// LOG.info("!!! Required of Attribute [" + pi.getName(false) + "] is [" + attributeUse.isRequired() + "].");
			}
			return attributeUse.isRequired();
		} else {
			// LOG.info("Field [" + pi.getName(false) + "] is of [" + pi.getSchemaComponent().getClass() + "].");
			return false;
		}
	}

	/**
	 * Retrieves a Collection of the fields defined by the given class, excluding all fields defined by its ancestor classes.
	 */
	public static Collection<JFieldVar> getConstructorRequiredFields(final ClassOutline classOutline) {
		final Map<String, JFieldVar> fields = classOutline.implClass.fields();
		final Collection<JFieldVar> fieldList = new ArrayList<JFieldVar>();
		for (final FieldOutline fo : classOutline.getDeclaredFields()) {
			if (!isFieldMarkedAsRequiredOrMandatoryInXSDSchema(fo)) {
				continue;
			}
			final JFieldVar var = fields.get(fo.getPropertyInfo().getName(false));
			if (var == null) {
				continue;
			}

			if (!isRelevantField(var)) {
				continue;
			}
			fieldList.add(var);
		}
		return fieldList;
	}
	
	public static Map<String, FieldOutline> getRequiredFieldsAsMap(final ClassOutline classOutline) {
		final Map<String, JFieldVar> fields = classOutline.implClass.fields();
		final Map<String, FieldOutline> fieldList = new HashMap<String, FieldOutline>();
		for (final FieldOutline fo : classOutline.getDeclaredFields()) {
			if (!isFieldMarkedAsRequiredOrMandatoryInXSDSchema(fo)) {
				continue;
			}
			final JFieldVar var = fields.get(fo.getPropertyInfo().getName(false));
			if (var == null) {
				continue;
			}

			if (!isRelevantField(var)) {
				continue;
			}
			fieldList.put(var.name(), fo);
		}
		return fieldList;
	}



	public static void findSubclasses(final CClassInfo cc, final ArrayList<CClassInfo> listSubclasses) {
		final Iterator<CClassInfo> subclasses = cc.listSubclasses();
		while (subclasses.hasNext()) {
			final CClassInfo s = subclasses.next();
			if (s.isAbstract()) {
				findSubclasses(s, listSubclasses);
				continue;
			}
			listSubclasses.add(s);
		}
	}

	public static HashMap<String, ArrayList<CClassInfo>> findSubClasses(final Outline outline) {
		// Logger LOG = Logger.getLogger(MoveAbstractClassesInOwnPackage.class.getName());
		final HashMap<String, ArrayList<CClassInfo>> peter = new HashMap<String, ArrayList<CClassInfo>>();
		logInfo("search for classses with subclasses: ");
		for (final ClassOutline classOutline : outline.getClasses()) {
			final ClassOutlineImpl cc = (ClassOutlineImpl) classOutline;
			final String currentClass = Util.eliminateTypeSuffix(cc.target.shortName);

			final StringBuffer sb = new StringBuffer();
			final ArrayList<CClassInfo> listSubclasses = new ArrayList<CClassInfo>();
			findSubclasses(cc.target, listSubclasses);
			peter.put(currentClass, listSubclasses);

			// from here only debug stuff
			sb.append(" > " + currentClass + " \t");
			if (listSubclasses.size() == 0) {
				continue;
			}
			sb.append("[");
			for (final CClassInfo cClassInfo : listSubclasses) {
				sb.append(cClassInfo.shortName + ", ");
			}
			if (listSubclasses.size() > 0) {
				sb.delete(sb.length() - 2, sb.length());
			}
			sb.append("]");
			logInfo(sb.toString());

		}

		return peter;
	}

}
