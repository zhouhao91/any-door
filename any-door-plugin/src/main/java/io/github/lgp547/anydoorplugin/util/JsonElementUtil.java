package io.github.lgp547.anydoorplugin.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class JsonElementUtil {
    public static boolean isNoSupportType(PsiClass psiClass) {
        return psiClass.isEnum();
    }

    public static JsonElement toJson(PsiType type, Integer num) {
        if (num > 5) {
            return JsonNull.INSTANCE;
        }
        if (type.isAssignableFrom(PsiType.INT)) {
            return new JsonPrimitive(0);
        }
        if (type.isAssignableFrom(PsiType.LONG)) {
            return new JsonPrimitive(0);
        }
        if (type.isAssignableFrom(PsiType.BOOLEAN)) {
            return new JsonPrimitive(false);
        }
        if (type.isAssignableFrom(PsiType.BYTE)) {
            return new JsonPrimitive("");
        }
        if (type.isAssignableFrom(PsiType.CHAR)) {
            return new JsonPrimitive("");
        }
        if (type.isAssignableFrom(PsiType.DOUBLE)) {
            return new JsonPrimitive(0.00);
        }
        if (type.isAssignableFrom(PsiType.FLOAT)) {
            return new JsonPrimitive(0.0);
        }
        if (type.isAssignableFrom(PsiType.SHORT)) {
            return new JsonPrimitive("");
        }
        if (type instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) type).resolve();
            if (null != psiClass) {
                if (isNoSupportType(psiClass)) {
                    return JsonNull.INSTANCE;
                } else {
                    try {
                        Class<?> aClass = Class.forName(psiClass.getQualifiedName());
                        if (isSimpleValueType(aClass)) {
                            return new JsonPrimitive("");
                        }
                        if (isCollType(aClass)) {
                            JsonArray jsonElements = new JsonArray();
                            Arrays.stream(((PsiClassType) type).getParameters()).map(i -> toJson(i, num + 1)).forEach(jsonElements::add);
                            return jsonElements;
                        }
                        if (isMapType(aClass)) {
                            JsonObject jsonObject = new JsonObject();
                            PsiType[] parameters = ((PsiClassType) type).getParameters();
                            if (parameters.length > 1 && num < 5) {
                                JsonElement key = toJson(parameters[0], num + 1);
                                JsonElement value = toJson(parameters[1], num + 1);
                                jsonObject.add(key instanceof JsonPrimitive ? key.getAsString() : key.toString(), value);
                            }
                            return jsonObject;
                        }
                    } catch (Exception ignored) {
                    }
                    if (psiClass.isInterface()) {
                        return JsonNull.INSTANCE;
                    }
                    JsonObject jsonObject1 = new JsonObject();
                    Arrays.stream(psiClass.getFields()).forEach(field -> {
                        if (!StringUtils.contains(field.getText(), " static ") && num < 5) {
                            jsonObject1.add(field.getName(), toJson(field.getType(), num + 1));
                        }
                    });
                    return jsonObject1;
                }
            }
        }


        return JsonNull.INSTANCE;
    }

    public static boolean isSimpleValueType(Class<?> type) {
        return (Void.class != type && void.class != type &&
                (ClassUtils.isPrimitiveOrWrapper(type) ||
                        Enum.class.isAssignableFrom(type) ||
                        CharSequence.class.isAssignableFrom(type) ||
                        Number.class.isAssignableFrom(type) ||
                        Date.class.isAssignableFrom(type) ||
                        Temporal.class.isAssignableFrom(type) ||
                        URI.class == type ||
                        URL.class == type ||
                        Locale.class == type ||
                        Class.class == type));
    }

    public static boolean isCollType(Class<?> type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }

    public static boolean isMapType(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    public static String getJsonText(PsiParameterList psiParameterList1) {
        JsonObject jsonObject = toParamNameListNew(psiParameterList1);
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return gson.toJson(jsonObject);
    }

    private static JsonObject toParamNameListNew(PsiParameterList parameterList) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < parameterList.getParametersCount(); i++) {
            PsiParameter parameter = Objects.requireNonNull(parameterList.getParameter(i));
            String key = parameter.getName();

            PsiType type = parameter.getType();
            JsonElement jsonElement = JsonElementUtil.toJson(type, 0);
            jsonObject.add(key, jsonElement);
        }
        return jsonObject;
    }

    public static String getSimpleText(PsiParameterList parameterList) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < parameterList.getParametersCount(); i++) {
            PsiParameter parameter = Objects.requireNonNull(parameterList.getParameter(i));
            String key = parameter.getName();
            jsonObject.add(key, new JsonObject());
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return gson.toJson(jsonObject);
    }
}
