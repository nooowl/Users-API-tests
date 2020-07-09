package com.nordigy.testrestapi.init;

import com.nordigy.testrestapi.model.User;
import com.nordigy.testrestapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log =  LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    private static final List<String> firstNames = List.of("John", "Robert", "Nataly", "Mary", "Alex", "Mark");
    private static final List<String> lastNames = List.of("Doe", "Smith", "Portman", "Li", "Erickson", "Roach");

    @Override
    public void run(String... args) {
        Random random = new Random();
        int firstNamesSize = firstNames.size();
        int lastNamesSize = lastNames.size();
        IntStream.rangeClosed(1, 20)
                 .mapToObj(buildRandomUser(random, firstNamesSize, lastNamesSize))
                 .forEach(userRepository::save);

        userRepository.findAll().forEach(user -> log.info(user.toString()));
    }

    private IntFunction<User> buildRandomUser(Random random, int firstNamesSize, int lastNamesSize) {
        return i -> User.builder()
                        .firstName(firstNames.get(random.nextInt(firstNamesSize)))
                        .lastName(lastNames.get(random.nextInt(lastNamesSize)))
                        .dayOfBirth(LocalDate.now().minus(20 + random.nextInt(70), ChronoUnit.YEARS))
                        .email(String.format("workingemail-%s@gmail.com", i))
                        .build();
    }
}
