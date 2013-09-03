package com.xxx.easyxml;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

// inspire by simplexml
public class EasyXml {

	public static <T> T parse(Class<? extends T> type, final String doc)
			throws Exception {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		xpp.setInput(new StringReader(doc));

		int eventType = xpp.next();
		if (eventType != XmlPullParser.START_TAG)
			throw new Exception("invalid xml");
		String tag = xpp.getName();
		EasyRoot root = getAnnotation(type, EasyRoot.class);
		if (root == null || !tag.equalsIgnoreCase(root.name()))
			throw new Exception("not found EasyRoot annotation in " + type.getName());
		return parseElement(type, xpp);
	}

	private static <T> T parseElement(Class<? extends T> type, XmlPullParser xpp)
			throws Exception {
		xpp.next();
		T t = type.newInstance();
		HashMap<String, Field> elementFields = new HashMap<String, Field>();
		HashMap<String, Field> elementListFields = new HashMap<String, Field>();
		for (Field field : type.getFields()) {
			// handle easy attribute field
			EasyAttribute attribute = getAnnotation(field, EasyAttribute.class);
			if (attribute != null) {
				String value = xpp.getAttributeValue(null, attribute.name());
				setFieldValue(t, field, value);
				continue;
			}
			EasyElement element = getAnnotation(field, EasyElement.class);
			if (element != null) {
				elementFields.put(element.name(), field);
			}
			EasyElementList elementList = getAnnotation(field,
					EasyElementList.class);
			if (elementList != null) {
				elementListFields.put(getElementListName(elementList), field);
			}
		}
		while (stepToTag(xpp)) {
			String name = xpp.getName();
			Field field = null;
			field = elementFields.remove(name);
			if (field != null) {
				field.set(t, parseElement(field.getType(), xpp));
				continue;
			}
			field = elementListFields.remove(name);
			if (field != null) {
				EasyElementList elementList = getAnnotation(field,
						EasyElementList.class);
				field.set(t, parseElementList(elementList, xpp));
				continue;
			}
			stepOutTag(xpp);
		}
		if (elementFields.size() > 0 || elementListFields.size() > 0)
			throw new Exception("element field miss in xml");
		stepOutTag(xpp);
		return t;
	}
	
	private static String getElementListName(final EasyElementList elementList) throws Exception {
		String name = "";
		if (elementList.inline()) {
			EasyRoot root = getAnnotation(elementList.type(), EasyRoot.class);
			if (root == null)
				throw new Exception("no EasyRoot annotation in atom of elementList");
			name = root.name();
			
		} else {
			name = elementList.name();
		}	
		if (name.length() <= 0)
			throw new Exception("empty name");
		return name;
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> parseElementList(
			final EasyElementList elementList, XmlPullParser xpp)
			throws Exception {
		boolean inline = elementList.inline();
		if (!inline) {
			xpp.next(); // step into
		}
		List<T> lst = new ArrayList<T>();
		while (stepToTag(xpp)) {
			if (inline) {
				String name = xpp.getName();
				if (!name.equalsIgnoreCase(getElementListName(elementList)))
					break;
			}
			lst.add((T) parseElement(elementList.type(), xpp));
		}
		if (!inline) {
			stepOutTag(xpp);
		}
		return lst;
	}

	// forward to next tag
	private static boolean stepToTag(XmlPullParser xpp) throws Exception {
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				return true;
			case XmlPullParser.END_TAG:
				return false;

			default:
				break;
			}
			eventType = xpp.next();
			if (eventType == XmlPullParser.END_DOCUMENT)
				throw new Exception("unexpected end document");
		}
		return false;
	}

	private static void stepOutTag(XmlPullParser xpp) throws Exception {
		if (xpp.getEventType() == XmlPullParser.END_TAG) {
			xpp.next();
			return;
		}
		int depth = 1;
		int eventType = xpp.next();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				depth += 1;
				break;
			case XmlPullParser.END_TAG:
				depth -= 1;
				if (depth == 0) {
					xpp.next();
					return;
				}
				break;

			default:
				break;
			}

			eventType = xpp.next();
			if (eventType == XmlPullParser.END_DOCUMENT)
				throw new Exception("unexpected end document");
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Annotation> T getAnnotation(
			AnnotatedElement elem, Class<? extends T> type) {
		for (Annotation v : elem.getAnnotations()) {
			if (v.annotationType().equals(type)) {
				return (T) v;
			}
		}
		return null;
	}

	private static void setFieldValue(Object object, Field field,
			final String value) throws Exception {
		if (field.getType().equals(String.class)) {
			field.set(object, value);
		} else if (field.getType().equals(Integer.class)) {
			field.set(object, Integer.parseInt(value));
		} else
			throw new Exception("not imp yet");
	}
}
