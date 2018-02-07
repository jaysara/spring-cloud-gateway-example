package com.example.edgeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@EnableWebFluxSecurity
@SpringBootApplication
public class EdgeServiceApplication {

//	@Bean
//	ApplicationRunner client(){
//		return args -> {
//			WebClient client =
//			WebClient.builder().filter(ExchangeFilterFunctions.basicAuthentication("user","password")).build();
//
//			Flux.fromStream(IntStream.range(0,100).boxed())
//					.flatMap(number -> client.get().uri("http://localhost:8081/rl").exchange())
//					.flatMap(clientResponse -> clientResponse.toEntity(String.class).map(re -> String.format("status %s  body %s",
//							re.getStatusCodeValue(),re.getBody()))).subscribe(System.out::println);
//		};
//	}

	@Bean
	DiscoveryClientRouteDefinitionLocator discoveryRoutes(DiscoveryClient dc)
	{
		return  new DiscoveryClientRouteDefinitionLocator(dc);
	}

	@Bean
	SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
		return http.httpBasic().and()
				.authorizeExchange()
				.pathMatchers("/rl").authenticated()
				.anyExchange().permitAll()
				.and()
				.build();
	}

	//authentication
	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService() {
		UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build();
		return new MapReactiveUserDetailsService(user);
	}



	@Bean
	RouteLocator gatewayRoutes(RouteLocatorBuilder builder, RequestRateLimiterGatewayFilterFactory rl){

		return builder.routes().route(
				r -> r.path("/get")
				.addRequestHeader("X-Spring","Cool")
				.uri("http://httpbin.org:80")
		).route(r -> r.path("/start")

					.uri("http://start.spring.io:80/")
		).route("lb",r -> r.path("/lb")
				//.uri("http://start.spring.io:80/")
				.uri("lb://customer-service/customers")

		).route("cf1",r -> r.path("/cf1")
				.filter((exchange,chain) ->
								chain.filter(exchange)
				.then(Mono.fromRunnable(()->
				{
					ServerHttpResponse httpResponse = exchange.getResponse();
					httpResponse.setStatusCode(HttpStatus.CONFLICT);
					httpResponse.getHeaders().setContentType(MediaType.APPLICATION_PDF);
				}))).uri("http://localhost:8081/customer-service/customers")
				)
		.route("cf2", r->r.path("/cf2/**")
						.rewritePath("/cf2/(?<CID>.*)","/customers/${CID}")
				.uri("lb://customer-service")
		)
				//circuit breaker
		.route("cb",r->r.path("/cb")
				.hystrix("cb")
				.uri("http://localhost:8081/customer-service/delay")
		)
		//rate limiter
		.route("rl", r->r.path("/rl")
				.filter(rl.apply(RedisRateLimiter.args(5,10)))

				.uri("http://localhost:8081/customer-service/customers")
		)
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(EdgeServiceApplication.class, args);
	}
}
