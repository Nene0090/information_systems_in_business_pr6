/** Параметры служебной заявки. */
public record Request(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
}
