package io.github.lgp547.anydoor.attach;

import io.github.lgp547.anydoor.attach.vmtool.AnyDoorVmToolUtils;
import io.github.lgp547.anydoor.common.dto.AnyDoorRunDto;
import io.github.lgp547.anydoor.common.util.AnyDoorBeanUtil;
import io.github.lgp547.anydoor.common.util.AnyDoorClassUtil;
import io.github.lgp547.anydoor.common.util.AnyDoorClassloader;
import io.github.lgp547.anydoor.common.util.AnyDoorFileUtil;
import io.github.lgp547.anydoor.common.util.AnyDoorSpringUtil;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class AnyDoorAttach {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        if (null == agentArgs || agentArgs.isEmpty()) {
            System.err.println("any_door agentmain param is empty");
            return;
        }

        if (agentArgs.startsWith("file://")) {
            try {
                agentArgs = AnyDoorFileUtil.getTextFileAsString(new File(agentArgs.substring(7)));
            } catch (IOException e) {
                System.err.println("any_door agentmain error. agentArgs[" + agentArgs + "]");
                e.printStackTrace();
                return;
            }
        }

        AnyDoorRun(agentArgs);
    }


    /**
     * {@link io.github.lgp547.anydoor.core.AnyDoorService#run(String, Method, Object)}
     */
    @SuppressWarnings("JavadocReference")
    public static void AnyDoorRun(String anyDoorDtoStr) {
        AnyDoorRunDto anyDoorRunDto = AnyDoorRunDto.parseObj(anyDoorDtoStr);
        if (!anyDoorRunDto.verifyPassByAttach()) {
            System.err.println("any_door agentmain error. anyDoorDtoStr[" + anyDoorDtoStr + "]");
            return;
        }

        AnyDoorVmToolUtils.init();


        try (AnyDoorClassloader anyDoorClassloader = new AnyDoorClassloader(anyDoorRunDto.getJarPaths())) {
            Class<?> clazz = AnyDoorClassUtil.forName(anyDoorRunDto.getClassName());
            Method method = AnyDoorClassUtil.getMethod(clazz, anyDoorRunDto.getMethodName(), anyDoorRunDto.getParameterTypes());
            Object instance = getInstance(clazz);

            Class<?> anyDoorServiceClass = anyDoorClassloader.loadClass("io.github.lgp547.anydoor.core.AnyDoorService");
            Object anyDoorService = anyDoorServiceClass.getConstructor().newInstance();
            Method run = anyDoorServiceClass.getMethod("run", String.class, Method.class, Object.class);
            run.invoke(anyDoorService, anyDoorDtoStr, method, instance);
        } catch (Exception e) {
            System.err.println("any_door agentmain error. anyDoorDtoStr[" + anyDoorDtoStr + "]");
            e.printStackTrace();
        }
    }

    private static Object getInstance(Class<?> clazz) {
        Object[] instances = AnyDoorVmToolUtils.getInstances(clazz);
        if (instances.length == 0) {
            // 尝试使用spring上下文获取
            AnyDoorSpringUtil.initApplicationContexts(() -> AnyDoorVmToolUtils.getInstances(ApplicationContext.class));
            if (AnyDoorSpringUtil.containsBean(clazz)) {
                return AnyDoorSpringUtil.getBean(clazz);
            } else {
                return AnyDoorBeanUtil.instantiate(clazz);
            }
        } else {
            return instances[0];
        }
    }


}