package io.github.crabzilla.example1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.example1.customer.CustomerData;
import io.vertx.core.json.Json;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class JacksonJsonTest {

  ObjectMapper mapper = Json.mapper;

  @Before
  public void setUp() {
    mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
  }

  @Test
  public void oneEvent() throws Exception {

    val id = new CustomerData.CustomerId(UUID.randomUUID().toString());
    val command = new CustomerData.CreateCustomer(UUID.randomUUID(), id, "customer1");
    val event = new CustomerData.CustomerCreated(id, command.getName());
    val uow1 = EntityUnitOfWork.unitOfWork(command, Version.create(1), Collections.singletonList(event));

    val uowAsJson = mapper.writeValueAsString(uow1);

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork.class);

    assertThat(uow2).isEqualTo(uow1);

  }

  @Test
  public void moreEvents() throws Exception {

    val id = new CustomerData.CustomerId("customer#1");
    val command = new CustomerData.CreateCustomer(UUID.randomUUID(), id, "customer1");
    val event1 = new CustomerData.CustomerCreated(id, command.getName());
    val event2 = new CustomerData.CustomerActivated("a rgood reason", Instant.now());

    val uow1 = EntityUnitOfWork.unitOfWork(command, Version.create(1), asList(event1,  event2));

    val uowAsJson = mapper.writeValueAsString(uow1);

//    System.out.println(uowAsJson);

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork.class);

    assertThat(uow2).isEqualTo(uow1);

  }

}
