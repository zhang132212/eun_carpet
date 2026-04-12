package com.eun.carpet.pearlcannon;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

public class PearlCannonScheme {
    private static final Logger LOGGER = LoggerFactory.getLogger("EUNCarpet|PearlCannon");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Field.class, new FieldDeserializer())
            .create();

    private String name;
    private int totalBits;
    private List<Field> fields;
    @SerializedName("outputOrder")
    private List<String> outputOrder;

    private transient Map<String, Field> fieldMap;

    public static PearlCannonScheme fromJson(String fileName, String jsonContent) throws JsonParseException {
        PearlCannonScheme scheme = GSON.fromJson(jsonContent, PearlCannonScheme.class);
        scheme.name = fileName.replace(".json", "");
        scheme.validate();
        return scheme;
    }

    private void validate() {
        if (totalBits <= 0) throw new JsonParseException("totalBits 必须大于0");
        if (fields == null || fields.isEmpty()) throw new JsonParseException("fields 不能为空");

        int sumBits = fields.stream().mapToInt(Field::getBits).sum();
        if (sumBits != totalBits) {
            throw new JsonParseException("fields 位数之和 (" + sumBits + ") 不等于 totalBits (" + totalBits + ")");
        }

        fieldMap = new HashMap<>();
        for (Field field : fields) {
            if (fieldMap.containsKey(field.getName())) {
                throw new JsonParseException("重复的字段名: " + field.getName());
            }
            fieldMap.put(field.getName(), field);
        }

        if (outputOrder == null || outputOrder.isEmpty()) {
            outputOrder = fields.stream().map(Field::getName).toList();
        } else {
            for (String name : outputOrder) {
                if (!fieldMap.containsKey(name)) {
                    throw new JsonParseException("outputOrder 中包含未知字段: " + name);
                }
            }
        }
    }

    public String getName() { return name; }
    public int getTotalBits() { return totalBits; }
    public List<Field> getFields() { return fields; }
    public List<String> getOutputOrder() { return outputOrder; }
    public Field getField(String name) { return fieldMap.get(name); }

    public static abstract class Field {
        private String name;
        private int bits;
        @SerializedName("displayName")
        private String displayName;

        public String getName() { return name; }
        public int getBits() { return bits; }
        public String getDisplayName() { return displayName != null ? displayName : name; }

        public abstract FieldType getType();
        public abstract Component validateInput(String input);
        public abstract String encode(String input) throws IllegalArgumentException;
    }

    public enum FieldType { SUM, ENUM }

    public static class SumField extends Field {
        private int[] weights;
        private transient int maxSum;

        @Override
        public FieldType getType() { return FieldType.SUM; }

        @Override
        public Component validateInput(String input) {
            try {
                int value = Integer.parseInt(input);
                if (value < 0 || value > maxSum) {
                    return Component.literal("输入值必须在 0 到 " + maxSum + " 之间");
                }
                return null;
            } catch (NumberFormatException e) {
                return Component.literal("必须输入整数");
            }
        }

        @Override
        public String encode(String input) throws IllegalArgumentException {
            int value = Integer.parseInt(input);
            return PearlCannonCalculator.encodeWithWeights(value, weights, getBits());
        }

        private void init() {
            if (weights.length != getBits()) {
                throw new JsonParseException("SUM字段 " + getName() + " 的 weights 长度必须等于 bits");
            }
            maxSum = Arrays.stream(weights).sum();
        }

        public int[] getWeights() { return weights; }
        public int getMaxSum() { return maxSum; }
    }

    public static class EnumField extends Field {
        private Map<String, String> mapping;
        private transient Map<String, String> reverseMapping;

        @Override
        public FieldType getType() { return FieldType.ENUM; }

        @Override
        public Component validateInput(String input) {
            if (!mapping.containsKey(input)) {
                return Component.literal("可选值: " + String.join(", ", mapping.keySet()));
            }
            return null;
        }

        @Override
        public String encode(String input) throws IllegalArgumentException {
            String binary = mapping.get(input);
            if (binary == null) throw new IllegalArgumentException("未知的枚举值: " + input);
            if (binary.length() != getBits()) {
                throw new IllegalStateException("枚举映射二进制串长度必须等于 bits，请检查配置文件");
            }
            return binary;
        }

        private void init() {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                if (entry.getValue().length() != getBits()) {
                    throw new JsonParseException("枚举字段 " + getName() + " 的映射值 '" + entry.getValue() + "' 长度不等于 " + getBits());
                }
                if (!entry.getValue().matches("[01]+")) {
                    throw new JsonParseException("枚举字段 " + getName() + " 的映射值只能包含 0 和 1");
                }
            }
        }

        public Set<String> getValidInputs() { return mapping.keySet(); }
    }

    private static class FieldDeserializer implements JsonDeserializer<Field> {
        @Override
        public Field deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String typeStr = obj.get("type").getAsString();
            Field field;
            if ("sum".equalsIgnoreCase(typeStr)) {
                field = context.deserialize(json, SumField.class);
            } else if ("enum".equalsIgnoreCase(typeStr)) {
                field = context.deserialize(json, EnumField.class);
            } else {
                throw new JsonParseException("未知的字段类型: " + typeStr);
            }
            if (field instanceof SumField sf) sf.init();
            if (field instanceof EnumField ef) ef.init();
            return field;
        }
    }
}