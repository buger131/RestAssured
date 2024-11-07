package api.extensions;

import api.annotations.User;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static utils.Props.getProperty;

public class BeforeEachExtension implements BeforeEachCallback {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeEachExtension.class);
    public static User user;

    @Override
    public void beforeEach(@NotNull ExtensionContext extensionContext) {
        user = getCurrentUser(extensionContext);
        LOG.debug("Запуск тестового сценария");
    }

    private User getCurrentUser(ExtensionContext extensionContext) {
        Method method = extensionContext.getRequiredTestMethod();
        Class<?> cl = extensionContext.getRequiredTestClass();

        if (AnnotationUtils.isAnnotated(method, User.class)) {
            return method.getAnnotation(User.class);
        } else if (AnnotationUtils.isAnnotated(method, User.class)) {
            Object[] args = getCurrentArgs(extensionContext);
            for (Object arg : args) {
                try {
                    return (User) arg;
                } catch (ClassCastException ignored) {}
            }
        } else if (AnnotationUtils.isAnnotated(cl, User.class)) {
            return cl.getAnnotation(User.class);
        } else {
            return new User() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return User.class;
                }

                @Override
                public String login() {
                    return getProperty("username");
                }

                @Override
                public String password() {
                    return getProperty("password");
                }
            };
        }
        throw new RuntimeException("Something went wrong");
    }

    private Object[] getCurrentArgs(ExtensionContext extensionContext) {
        Method method = extensionContext.getRequiredTestMethod();
        AtomicLong invocationCount = new AtomicLong(0L);
        List<Object[]> all_args = AnnotationUtils.findRepeatableAnnotations(method, ArgumentsSource.class).stream().map(ArgumentsSource::value).map(ReflectionUtils::newInstance).map((provider) -> AnnotationConsumerInitializer.initialize(method, provider)).flatMap((provider) -> {
            try {
                return provider.provideArguments(extensionContext);
            } catch (Exception e) {
                throw ExceptionUtils.throwAsUncheckedException(e);
            }
        }).map(Arguments::get).peek(arguments -> invocationCount.incrementAndGet()).toList();

        return all_args.get(getCurrentIndex(extensionContext));
    }

    private int getCurrentIndex(ExtensionContext extensionContext) {
        String displayName = extensionContext.getDisplayName();
        String[] parts = displayName.split("\\[");
        if (parts.length > 1) {
            String numberString = parts[1].split("]")[0];
            return Integer.parseInt(numberString) - 1;
        }
        return -1;
    }
}
