package crabzilla.model;

import java.io.Serializable;

public interface AggregateRoot extends Serializable {

  AggregateRootId getId();

}