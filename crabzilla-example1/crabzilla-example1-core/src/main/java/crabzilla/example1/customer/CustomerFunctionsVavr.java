package crabzilla.example1.customer;

import crabzilla.model.*;
import crabzilla.stack.AbstractCommandValidatorFn;
import crabzilla.stack.UnknownCommandException;
import io.vavr.Function3;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static crabzilla.example1.customer.CustomerData.*;
import static crabzilla.model.EntityUnitOfWork.unitOfWork;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.util.Collections.emptyList;

public class CustomerFunctionsVavr {
  
  public static class SupplierFn implements Supplier<Customer> {
    final Customer customer = new Customer(null, null,  null, false, null);
    @Override
    public Customer get() {
      return customer;
    }
  }

  public static class StateTransitionFn implements BiFunction<DomainEvent, Customer, Customer> {

    public Customer apply(final DomainEvent event, final Customer instance) {

      return Match(event).of(
        Case($(instanceOf(CustomerCreated.class)),
                (e) -> instance.withId(e.getId()).withName(e.getName())),
        Case($(instanceOf(CustomerActivated.class)),
                (e) -> instance.withReason(e.getReason()).withActive(true)),
        Case($(instanceOf(CustomerDeactivated.class)),
                (e) -> instance.withReason(e.getReason()).withActive(false)),
        Case($(), o -> instance));

    }
  }

  // TODO consider an example with some real business logic (a CreditService, for example)
  @Slf4j
  public static class CommandHandlerFn
          implements BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> {

    final StateTransitionsTrackerFactory<Customer> trackerFactory;

    public CommandHandlerFn(StateTransitionsTrackerFactory<Customer> trackerFactory) {
      this.trackerFactory = trackerFactory;
    }

    @Override
    public CommandHandlerResult apply(final EntityCommand cmd, final Snapshot<Customer> snapshot) {

      CommandHandlerFn.log.info("Will apply command {}", cmd);

      try {
        return CommandHandlerResult.success(handle(cmd, snapshot));
      } catch (Exception e) {
        return CommandHandlerResult.error(e);
      }

    }

    private EntityUnitOfWork handle(final EntityCommand cmd, final Snapshot<Customer> snapshot) {

      val targetInstance = snapshot.getInstance();
      val targetVersion = snapshot.getVersion();

      final EntityUnitOfWork uow = Match(cmd).of(

        Case($(instanceOf(CreateCustomer.class)), (command) ->
          unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.create(command.getTargetId(), command.getName()))
        ),

        Case($(instanceOf(ActivateCustomer.class)), (command) ->
          unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.activate(command.getReason()))),

        Case($(instanceOf(DeactivateCustomer.class)), (command) ->
          unitOfWork(cmd, targetVersion.nextVersion(), targetInstance.deactivate(command.getReason()))),

        Case($(instanceOf(CreateActivateCustomer.class)), (command) -> {
          val tracker = trackerFactory.create(targetInstance);
          val events = tracker
                  .applyEvents(customer -> customer.create(command.getTargetId(), command.getName()))
                  .applyEvents(customer -> customer.activate(command.getReason()))
                  .collectEvents();

          return unitOfWork(cmd, targetVersion.nextVersion(), events);

        }),

        Case($(), o -> {
          throw new UnknownCommandException("for command " + cmd.getClass().getSimpleName());

        })

      );

      return uow;

    }

  }

  public static class CommandValidatorFn extends AbstractCommandValidatorFn {

      public java.util.List<String> validate(CreateCustomer cmd) {

        val either = new CreateCustomerValidator().validate(cmd).toEither();

        return either.isRight() ? emptyList() : either.getLeft().asJava();

      }

    public java.util.List<String> validate(ActivateCustomer cmd) {

      return emptyList(); // it's always valid

    }
  }

  public static class CreateCustomerValidator {

    private static final String VALID_NAME_CHARS = "[a-zA-Z ]";

    public Validation<Seq<String>, CustomerData.CreateCustomer> validate(CreateCustomer cmd) {
      return Validation.combine(validateCmdId(cmd.getCommandId()),
              validateId(cmd.getTargetId()),
              validateName(cmd.getName())
      ).ap((Function3<UUID, CustomerId, String, CreateCustomer>) CreateCustomer::new);
    }

    private Validation<String, UUID>  validateCmdId(UUID commandId) {
      return commandId == null
              ? Validation.invalid("CommandId cannot be null ")
              : Validation.valid(commandId);
    }

    private Validation<String, CustomerData.CustomerId> validateId(CustomerId id) {
      return id == null
              ? Validation.invalid("CustomerId cannot be null ")
              : Validation.valid(id);
    }

    private Validation<String, String> validateName(String name) {
      return CharSeq.of(name).replaceAll(VALID_NAME_CHARS, "").transform(seq -> seq.isEmpty()
              ? Validation.valid(name)
              : Validation.invalid("Name contains invalid characters: '"
              + seq.distinct().sorted() + "'"));
    }

  }

}