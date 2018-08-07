package core.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonUtils {
    public JsonUtils() {
    }

    public static String ObjectToJson(Object obj) {
        return JSON.toJSONString(obj, new SerializerFeature[]{SerializerFeature.WriteClassName});
    }

    public static Object JsonToObject(String str) {
        return JSON.parse(str);
    }
}
