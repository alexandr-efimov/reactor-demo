package software.sigma.githubdemo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
public class GithubDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubDemoApplication.class, args);
    }


}

@Service
@Slf4j
class SynchronizedService {
    public List<String> getSomeList() {
        Random random = new Random();
        return IntStream.range(0, 5)
                .mapToObj(number -> "elem->" + random.nextInt())
                .peek(e -> {
                    try {
                        Thread.sleep(1000);
                        log.info("Current ---> " + e + ".  THREAD: " + Thread.currentThread());
                    } catch (InterruptedException e1) { //NOSONAR
                        log.error("Error, thread interrupted.", e);
                    }
                })
                .collect(Collectors.toList());
    }
}

@Service
@Slf4j
class ReactiveServiceImpl {

    //    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    @Autowired
    private SynchronizedService synchronizedService;

    List<String> doSomeReactiveActions() {
        List<String> result = new ArrayList<>();


        for (int i = 0; i < 5; i++) {
            Mono.fromCallable(() -> synchronizedService.getSomeList())
                    .subscribeOn(Schedulers.parallel())
                    .subscribe(strings -> {
                        log.info("Got: {}", strings);
                        result.addAll(strings);
                    });
        }
//
//        try {
//            Thread.sleep(6000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        log.info("Result size: {}, objects: {}, thread: {}", result.size(), result, Thread.currentThread());

        return result;
    }


}

@RestController
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
class ReactiveController {

    private ReactiveServiceImpl reactiveService;

    @GetMapping(path = "reactive")
    public List<String> testSomeReactivity() {
        log.info("hello from reactive...");
        return reactiveService.doSomeReactiveActions();
    }
}