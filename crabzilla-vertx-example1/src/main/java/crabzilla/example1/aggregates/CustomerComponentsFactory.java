package crabzilla.example1.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.services.SampleService;
import crabzilla.model.*;
import crabzilla.vertx.VertxAggregateRootComponentsFactory;
import crabzilla.vertx.repositories.VertxEventRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static crabzilla.vertx.util.StringHelper.circuitBreakerId;

public class CustomerComponentsFactory implements VertxAggregateRootComponentsFactory<Customer> {

  private final SampleService service;
  private final JDBCClient jdbcClient;
  private final Cache<String, Snapshot<Customer>> cache;
  private final CircuitBreaker circuitBreaker;
  private final Vertx vertx;
  private final VertxEventRepository eventStore;

  @Inject
  public CustomerComponentsFactory(SampleService service, JDBCClient jdbcClient, Vertx vertx,
                                   VertxEventRepository eventStore) {
    this.service = service;
    this.jdbcClient = jdbcClient;
    this.vertx = vertx;
    this.eventStore = eventStore;
    this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    this.circuitBreaker = CircuitBreaker.create(circuitBreakerId(Customer.class), vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );
  }

  @Override
  public Supplier<Customer> supplierFn() {
    return new CustomerSupplierFn();
  }

  @Override
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {return new CustomerStateTransitionFn(); }

  @Override
  public Function<Command, List<String>> cmdValidatorFn() {
    return new CustomerCommandValidatorFn();
  }

  @Override
  public BiFunction<Command, Snapshot<Customer>, Either<Exception, Optional<UnitOfWork>>> cmdHandlerFn() {
    return new CustomerCmdHandlerFn(stateTransitionFn(), depInjectionFn());
  }

  @Override
  public Cache<String, Snapshot<Customer>> cache() {
    return cache;
  }

  @Override
  public CircuitBreaker circuitBreaker() {
    return circuitBreaker;
  }

  @Override
  public CommandRestVerticle<Customer> restVerticle() {
    return new CommandRestVerticle<>(vertx, Customer.class);
  }

  @Override
  public CommandHandlerVerticle<Customer> cmdHandlerVerticle() {
    return new CommandHandlerVerticle<>(Customer.class, cmdHandlerFn(),
            cmdValidatorFn(), snaphotFactory(), eventStore,
            cache(), vertx, circuitBreaker);
  }

  @Override
  public Function<Customer, Customer> depInjectionFn() {return (c) -> c.withService(service); }

}
