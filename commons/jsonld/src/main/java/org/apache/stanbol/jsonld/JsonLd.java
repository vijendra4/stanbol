package org.apache.stanbol.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The JsonLd class provides an API to create a JSON-LD object structure and to serialize this structure.
 * 
 * <p>
 * This implementation is based on the JSON-LD specification version 20110201. Available online at <a
 * href="http://www.json-ld.org/spec/ED/20110201/">http://www.json-ld.org/spec/ED/20110201/</a>.
 * 
 * @author Fabian Christ
 */
public class JsonLd {

    public static final String CONTEXT = "@context";
    public static final String TYPES = "#types";
    public static final String PROFILE = "@profile";
    public static final String SUBJECT = "@";
    public static final String IRI = "@iri";

    // Map Namespace -> Prefix
    private Map<String,String> namespacePrefixMap = new HashMap<String,String>();

    // Map Subject -> Resource
    private Map<String,JsonLdResource> resourceMap = new TreeMap<String,JsonLdResource>(new JsonComparator());

    /**
     * Flag to control whether the namespace prefix map should be used to shorten URIs to CURIEs during
     * serialization. Default value is <code>true</code>.
     */
    private boolean applyNamespaces = true;

    /**
     * Flag to control whether the serialized JSON-LD output will use joint or disjoint graphs for subjects
     * and namespaces. Default value is <code>true</code>.
     */
    private boolean useJointGraphs = true;

    /**
     * Flag to control whether type coercion should be applied on serialization. Default value is
     * <code>false</code>.
     */
    private boolean useTypeCoercion = false;

    /**
     * Flag that indicates whether this JSON-LD object represents a JSON-LD profile.
     */
    private final boolean representsProfile;

    public JsonLd() {
        this.representsProfile = false;
    }

    public JsonLd(boolean representsProfile) {
        this.representsProfile = representsProfile;
    }

    /**
     * Adds the given resource to this JsonLd object using the resource's subject as key. If the key is NULL
     * and there does not exist a resource with an empty String as key the resource will be added using
     * an empty String ("") as key. Otherwise an @IllegalArgumentException is thrown.
     * 
     * @param resource
     */
    public void put(JsonLdResource resource) {
        if (resource.getSubject() != null) {
            this.resourceMap.put(resource.getSubject(), resource);
        }
        else if (!this.resourceMap.containsKey("")) {
            this.resourceMap.put("", resource);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Add the given resource to this JsonLd object using the resourceId as key.
     * 
     * @param resourceId
     * @param resource
     */
    public void put(String resourceId, JsonLdResource resource) {
        this.resourceMap.put(resourceId, resource);
    }

    /**
     * Serializes the JSON-LD object structures to a String.
     * 
     * <p>
     * If you want to have a formatted output with indentation, use the toString(int indent) variant.
     * 
     * @return JSON-LD as unformatted String.
     */
    @Override
    public String toString() {
        if (useJointGraphs) {
            Map<String,Object> json = createJointGraph();

            return JsonSerializer.toString(json);
        } else {
            List<Object> json = createDisjointGraph();

            return JsonSerializer.toString(json);
        }
    }

    /**
     * Serializes the JSON-LD object structure to a beautified String using indentation. The output is
     * formatted using the specified indentation size.
     * 
     * @param indent
     *            Number of whitespace chars used for indentation.
     * @return JSON-LD as formatted String.
     */
    public String toString(int indent) {
        if (useJointGraphs) {
            Map<String,Object> json = createJointGraph();

            return JsonSerializer.toString(json, indent);
        } else {
            List<Object> json = createDisjointGraph();

            return JsonSerializer.toString(json, indent);
        }
    }

    private List<Object> createDisjointGraph() {
        List<Object> json = new ArrayList<Object>();
        if (!resourceMap.isEmpty()) {

            for (String subject : resourceMap.keySet()) {
                Map<String,Object> subjectObject = new TreeMap<String,Object>(new JsonComparator());
                JsonLdResource resource = resourceMap.get(subject);

                // put the namespaces
                if (!this.namespacePrefixMap.isEmpty() || this.useTypeCoercion) {
                    Map<String,Object> nsObject = new TreeMap<String,Object>(new JsonComparator());
                    for (String ns : this.namespacePrefixMap.keySet()) {
                        nsObject.put(this.namespacePrefixMap.get(ns), ns);
                    }
                    if (this.useTypeCoercion) {
                        putCoercionTypes(nsObject, resource.getCoercionMap());
                    }
                    subjectObject.put("#", nsObject);
                }

                // put subject
                if (resource.getSubject() != null && !resource.getSubject().isEmpty()) {
                    subjectObject.put(SUBJECT, handleCURIEs(resource.getSubject()));
                }

                // put profile
                if (resource.getProfile() != null && !resource.getProfile().isEmpty()) {
                    subjectObject.put(PROFILE, handleCURIEs(resource.getProfile()));
                }

                // put types
                putTypes(subjectObject, resource);

                // put properties = objects
                putProperties(subjectObject, resource);

                // add to list of subjects
                json.add(subjectObject);
            }

        }

        return json;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> createJointGraph() {
        Map<String,Object> json = new TreeMap<String,Object>(new JsonComparator());
        Map<String,String> coercionMap = new TreeMap<String,String>(new JsonComparator());

        if (!resourceMap.isEmpty()) {
            List<Object> subjects = new ArrayList<Object>();

            for (String subject : resourceMap.keySet()) {
                // put subject
                Map<String,Object> subjectObject = new TreeMap<String,Object>(new JsonComparator());

                JsonLdResource resource = resourceMap.get(subject);

                // put subject if this is not a profile
                if (!this.representsProfile) {
                    if (resource.getSubject() != null && !resource.getSubject().isEmpty()) {
                        subjectObject.put(SUBJECT, handleCURIEs(resource.getSubject()));
                    }
                }

                // put profile
                if (resource.getProfile() != null && !resource.getProfile().isEmpty()) {
                    subjectObject.put(PROFILE, handleCURIEs(resource.getProfile()));
                }

                // put types
                putTypes(subjectObject, resource);

                if (this.useTypeCoercion) {
                    coercionMap.putAll(resource.getCoercionMap());
                }

                // put properties = objects
                putProperties(subjectObject, resource);

                // add to list of subjects
                subjects.add(subjectObject);
            }

            // put subjects
            if (!subjects.isEmpty()) {
                if (subjects.size() == 1) {
                    json = (Map<String,Object>) subjects.get(0);
                } else {
                    json.put("@", subjects);
                }
            }
        }

        // put the namespaces
        if (!this.namespacePrefixMap.isEmpty() || (this.useTypeCoercion && !coercionMap.isEmpty())) {

            Map<String,Object> nsObject = new TreeMap<String,Object>(new JsonComparator());
            for (String ns : namespacePrefixMap.keySet()) {
                nsObject.put(namespacePrefixMap.get(ns), ns);
            }

            if (this.useTypeCoercion && !coercionMap.isEmpty()) {
                putCoercionTypes(nsObject, coercionMap);
            }
            json.put("#", nsObject);
        }

        return json;
    }

    private void putTypes(Map<String,Object> subjectObject, JsonLdResource resource) {
        if (!resource.getTypes().isEmpty()) {
            List<String> types = new ArrayList<String>();
            for (String type : resource.getTypes()) {
                types.add(handleCURIEs(type));
            }
            if (types.size() == 1) {
                subjectObject.put("a", types.get(0));
            } else {
                Collections.sort(types, new Comparator<String>() {

                    @Override
                    public int compare(String arg0, String arg1) {
                        return arg0.compareTo(arg1);
                    }

                });
                subjectObject.put("a", types);
            }
        }
    }

    private void putCoercionTypes(Map<String,Object> jsonObject, Map<String,String> coercionMap) {
        if (!coercionMap.isEmpty()) {
            Map<String,String> nsCoercionMap = new TreeMap<String,String>(new JsonComparator());
            for (String property : coercionMap.keySet()) {
                nsCoercionMap.put(handleCURIEs(property), handleCURIEs(coercionMap.get(property)));
            }
            jsonObject.put("#types", nsCoercionMap);
        }
    }

    private void putProperties(Map<String,Object> jsonObject, JsonLdResource resource) {
        putProperties(jsonObject, resource.getPropertyMap(), resource.getCoercionMap());
    }

    private void putProperties(Map<String,Object> outputObject,
                               Map<String,Object> inputMap,
                               Map<String,String> coercionMap) {
        for (String property : inputMap.keySet()) {
            Object value = inputMap.get(property);
            if (value instanceof String) {
                String strValue = (String) value;
                if (coercionMap != null) {
                    String type = coercionMap.get(property);
                    if (type != null) {
                        if (this.useTypeCoercion) {
                            strValue = (String)doCoerce(strValue, type);
                        } else {
                            strValue = unCoerce(strValue, type);
                        }
                    }
                }
                value = handleCURIEs(strValue);
                outputObject.put(handleCURIEs(property), value);
            } else if (value instanceof Object[]) {
                Object[] arrayValue = (Object[]) value;
                putProperties(outputObject, property, arrayValue, coercionMap);
            } else if (value instanceof Map<?,?>) {
                Map<String,Object> valueMap = (Map<String,Object>) value;
                Map<String,Object> subOutputObject = new HashMap<String,Object>();
                outputObject.put(handleCURIEs(property), subOutputObject);
                putProperties(subOutputObject, valueMap, coercionMap);
            } else if (value instanceof JsonLdIRI) {
                JsonLdIRI iriValue = (JsonLdIRI) value;
                Map<String,Object> iriObject = new HashMap<String,Object>();
                iriObject.put("@iri", handleCURIEs(iriValue.getIRI()));
                outputObject.put(handleCURIEs(property), iriObject);
            } else {
                if (coercionMap != null) {
                    String type = coercionMap.get(property);
                    if (type != null) {
                        Object objValue = null;
                        if (this.useTypeCoercion) {
                            objValue = doCoerce(value.toString(), type);
                        } else {
                            objValue = unCoerce(value.toString(), type);
                        }
                        
                        if (objValue instanceof String) {
                            String strValue = (String) objValue;
                            outputObject.put(handleCURIEs(property), handleCURIEs(strValue));
                        }
                        else {
                            outputObject.put(handleCURIEs(property), objValue);
                        }
                    } else {
                        outputObject.put(handleCURIEs(property), value);
                    }
                } else {
                    outputObject.put(handleCURIEs(property), value);
                }
            }
        }
    }

    private void putProperties(Map<String,Object> outputObject,
                               String property,
                               Object[] arrayValue,
                               Map<String,String> coercionMap) {
        if (arrayValue instanceof String[]) {
            String[] stringArray = (String[]) arrayValue;
            List<String> valueList = new ArrayList<String>();
            for (String uri : stringArray) {
                valueList.add(handleCURIEs(uri));
            }
            outputObject.put(handleCURIEs(property), valueList);
        } else {
            List<Object> valueList = new ArrayList<Object>();
            for (Object object : arrayValue) {
                if (object instanceof Map<?,?>) {
                    // The value of an array element is a Map. Handle maps recursively.
                    Map<String,Object> inputMap = (Map<String,Object>) object;
                    Map<String,Object> subOutputObject = new HashMap<String,Object>();
                    valueList.add(subOutputObject);
                    putProperties(subOutputObject, inputMap, coercionMap);
                } else if (object instanceof JsonLdIRI) {
                    JsonLdIRI iriValue = (JsonLdIRI) object;
                    Map<String,Object> iriObject = new HashMap<String,Object>();
                    iriObject.put("@iri", handleCURIEs(iriValue.getIRI()));
                    valueList.add(iriObject);
                } else {
                    // Don't know what it is - just add it
                    valueList.add(object);
                }
            }

            // Add the converted values
            outputObject.put(handleCURIEs(property), valueList);
        }
    }

    /**
     * Appends the type to the Value if not present.
     * 
     * @param strValue
     * @param type
     * @return
     */
    private String unCoerce(String strValue, String type) {
        String typeSuffix = "^^" + unCURIE((type));
        if (!strValue.endsWith(typeSuffix)) {
            strValue = "\"" + strValue + "\"^^<" + type + ">";
        }
        return strValue;
    }

    /**
     * Removes the type from the value and handles conversion to Integer and Boolean.
     * 
     * @param strValue
     * @param type
     * @return
     */
    private Object doCoerce(String strValue, String type) {
        String typeSuffix = "^^" + unCURIE((type));
        strValue = strValue.replace(typeSuffix, "");
        strValue = strValue.replaceAll("\"", "");
        return convertValueType(strValue);
    }

    /**
     * Converts a given object to Integer or Boolean if the object is instance of one of those types.
     * 
     * @param strValue
     * @return
     */
    private Object convertValueType(String strValue) {
        // check if value can be interpreted as integer
        try {
            return Integer.valueOf(strValue);
        }
        catch (Throwable t) {};
        
        // check if value can be interpreted as boolean
        if (strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("false")) {
            return Boolean.valueOf(strValue);
        }
        
        return strValue;
    }
    
    /**
     * Convert URI to CURIE if namespaces should be applied and CURIEs to URIs if namespaces should not be
     * applied.
     * 
     * @param uri
     *            That may be in CURIE form.
     * @return
     */
    private String handleCURIEs(String uri) {
        if (this.applyNamespaces) {
            uri = doCURIE(uri);
        } else {
            uri = unCURIE(uri);
        }

        return uri;
    }

    private String doCURIE(String uri) {
        for (String namespace : namespacePrefixMap.keySet()) {
            String prefix = namespacePrefixMap.get(namespace) + ":";
            if (!uri.startsWith(prefix)) {
                uri = uri.replace(namespace, prefix);
            }
        }
        return uri;
    }

    private String unCURIE(String uri) {
        for (String namespace : namespacePrefixMap.keySet()) {
            String prefix = namespacePrefixMap.get(namespace) + ":";
            if (uri.startsWith(prefix)) {
                uri = uri.replace(prefix, namespace);
            }
        }
        return uri;
    }

    /**
     * Return the JSON-LD resource for the given subject.
     */
    public JsonLdResource getResource(String subject) {
        return resourceMap.get(subject);
    }

    /**
     * Get the known namespace to prefix mapping.
     * 
     * @return A {@link Map} from namespace String to prefix String.
     */
    public Map<String,String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }

    /**
     * Sets the known namespaces for the serializer.
     * 
     * @param namespacePrefixMap
     *            A {@link Map} from namespace String to prefix String.
     */
    public void setNamespacePrefixMap(Map<String,String> namespacePrefixMap) {
        this.namespacePrefixMap = namespacePrefixMap;
    }

    /**
     * Adds a new namespace and its prefix to the list of used namespaces for this JSON-LD instance.
     * 
     * @param namespace
     *            A namespace IRI.
     * @param prefix
     *            A prefix to use and identify this namespace in serialized JSON-LD.
     */
    public void addNamespacePrefix(String namespace, String prefix) {
        namespacePrefixMap.put(namespace, prefix);
    }

    /**
     * Determine whether currently joint or disjoint graphs are serialized with this JSON-LD instance.
     * 
     * @return <code>True</code> if joint graphs are used, <code>False</code>otherwise.
     */
    public boolean isUseJointGraphs() {
        return useJointGraphs;
    }

    /**
     * Set to <code>true</code> if you want to use joint graphs (default) or <code>false</code> otherwise.
     * 
     * @param useJointGraphs
     */
    public void setUseJointGraphs(boolean useJointGraphs) {
        this.useJointGraphs = useJointGraphs;
    }

    /**
     * Flag to control whether the namespace prefix map should be used to shorten IRIs to prefix notation
     * during serialization. Default value is <code>true</code>.
     * <p>
     * If you already put values into this JSON-LD instance with prefix notation, you should set this to
     * <code>false</code> before starting the serialization.
     * 
     * @return <code>True</code> if namespaces are applied during serialization, <code>false</code> otherwise.
     */
    public boolean isApplyNamespaces() {
        return applyNamespaces;
    }

    /**
     * Control whether namespaces from the namespace prefix map are applied to URLs during serialization.
     * <p>
     * Set this to <code>false</code> if you already have shortened IRIs with prefixes.
     * 
     * @param applyNamespaces
     */
    public void setApplyNamespaces(boolean applyNamespaces) {
        this.applyNamespaces = applyNamespaces;
    }

    /**
     * Flag to control whether type coercion is applied or not.
     * 
     * @return <code>True</code> if type coercion is applied, <code>false</code> otherwise.
     */
    public boolean isUseTypeCoercion() {
        return useTypeCoercion;
    }

    /**
     * Control whether type coercion should be applied. Set this to <code>false</code> if you don't want to
     * use type coercion in the output.
     * 
     * @param useTypeCoercion
     */
    public void setUseTypeCoercion(boolean useTypeCoercion) {
        this.useTypeCoercion = useTypeCoercion;
    }

    /**
     * Check whether this JSON-LD object represents a JSON-LD profile.
     * 
     * @return <code>true</code> if this is a profile, <code>false</code> otherwise.
     */
    public boolean representsProfile() {
        return this.representsProfile;
    }
}
