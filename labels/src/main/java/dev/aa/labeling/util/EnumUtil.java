package dev.aa.labeling.util;

import java.util.Arrays;
import java.util.List;

public class EnumUtil {

    public static <E extends Enum<E>> List<String> getValues(E e) {

        if (e == null)
            return null;

        return Arrays.stream(e.getDeclaringClass().getEnumConstants())
                .map(Enum::name)
                .toList();
    }


}
