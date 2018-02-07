package com.demo.example.customerservice;

import com.mongodb.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@SpringBootApplication
public class CustomerServiceApplication {

	@Bean
	ApplicationRunner init(CustomerRepository cr)
	{

		return args -> (Flux.just("A","B","C").map( l -> new Customer(null,l)).flatMap(x ->cr.save(x)))
				.thenMany(cr.findAll())
				.subscribe(System.out::println);

	}

	@Bean
	public com.mongodb.reactivestreams.client.MongoClient mongoClient() {
		//MongoClientSettings.builder().
		return com.mongodb.reactivestreams.client.MongoClients.create("mongodb://CPZDEVUSR_CPZ:changeMe123$@lxv1111.allstate.com:27010");

	}
	public @Bean ReactiveMongoTemplate reactiveMongoTemplate() {
		return new ReactiveMongoTemplate(mongoClient(), "CPZDEVENG");
	}

	@Bean
	RouterFunction<?> routes (CustomerRepository cr){
		return route(GET("/customers"), r-> ok().body(Flux.just("Hello Jay Simple"),String.class))
				//.andRoute(GET("/customers/{id}"), r ->ok().body(cr.findById(r.pathVariable("id")),Customer.class))
				.andRoute(GET("/customers/{id}"), r ->ok().body(Flux.just("Hello Jay FOR Customer "+r.pathVariable("id")),String.class))
				.andRoute(GET("/delay"),r ->ok().body( Flux.just("Hello Jay").delayElements(Duration.ofSeconds(10)),String.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceApplication.class, args);
	}
}



class CustomerList {

	public List<Customer> customerList = new ArrayList<>();
	public CustomerList()
	{
		customerList.add(new Customer(new Integer(RandomUtils.nextInt()).toString(),"A"));
		customerList.add(new Customer(new Integer(RandomUtils.nextInt()).toString(),"n"));
		customerList.add(new Customer(new Integer(RandomUtils.nextInt()).toString(),"c"));

	}

}

interface CustomerRepository extends ReactiveMongoRepository<Customer,String>
{

}



@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer{
	@Id
	private String id;

	private String name;
}
