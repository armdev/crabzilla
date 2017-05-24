package crabzilla.model;

import crabzilla.UnitOfWork;
import crabzilla.util.Either;
import crabzilla.util.Eithers;
import crabzilla.util.MultiMethod;
import lombok.NonNull;
import lombok.val;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class CommandHandlerFn<A extends AggregateRoot> {

  protected final BiFunction<Event, A, A> stateTransitionFn;
  protected final Function<A, A> dependencyInjectionFn;
  private final MultiMethod mm ;

  protected CommandHandlerFn(@NonNull BiFunction<Event, A, A> stateTransitionFn,
                             @NonNull Function<A, A> dependencyInjectionFn) {
    this.stateTransitionFn = stateTransitionFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "handle");
  }

  public Either<Exception, UnitOfWork> handle(final Command command, final Snapshot<A> snapshot) {

    try {
      val optUnitOfWork = ((UnitOfWork)mm.invoke(this, command, snapshot));
      return Eithers.right(optUnitOfWork);
    } catch (Exception e) {
      return Eithers.left(e);
    }

  }

}
