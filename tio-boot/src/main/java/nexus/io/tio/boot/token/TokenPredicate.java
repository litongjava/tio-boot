package nexus.io.tio.boot.token;

@FunctionalInterface
public interface TokenPredicate {
  public PredicateResult validate(String token);
}