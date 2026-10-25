/**
 * Выбор маршрута согласования. Все правила и пороги собраны в этом классе.
 *
 * Порядок проверок - от наиболее строгого правила к общему:
 *   1. персональные данные            -> SECURITY_REVIEW (независимо от суммы)
 *   2. риск HIGH                      -> DIRECTOR_REVIEW
 *   3. вариант 5: риск MEDIUM и сумма >= 30 % порога -> DIRECTOR_REVIEW
 *   4. сумма >= FINANCE_LIMIT         -> FINANCE_REVIEW
 *   5. срочная (и сумма < FINANCE_LIMIT, т.к. п. 4 уже отсёк большие) -> FAST_TRACK
 *   6. иначе                          -> STANDARD
 */
public class ApprovalRouter {

    /** Порог финансового согласования: сумма >= порога требует FINANCE_REVIEW. */
    private static final long FINANCE_LIMIT = 500_000L;

    /**
     * Вариант 5 ("приоритет риска", модификатор 30): повышение приоритета риска.
     * Заявка с риском MEDIUM и суммой не менее 30 % от FINANCE_LIMIT (150 000)
     * направляется на DIRECTOR_REVIEW, т.е. риск перекрывает финансовую проверку.
     */
    private static final int RISK_ESCALATION_PERCENT = 30;

    /**
     * @param amount               сумма заявки, не может быть отрицательной
     * @param urgent               признак срочности
     * @param risk                 уровень риска, не может быть null
     * @param containsPersonalData есть ли персональные данные
     * @throws IllegalArgumentException при отрицательной сумме или risk == null
     */
    public ApprovalRoute route(long amount, boolean urgent, RiskLevel risk, boolean containsPersonalData) {
        validate(amount, risk);

        if (containsPersonalData) {
            return ApprovalRoute.SECURITY_REVIEW;
        }
        if (risk == RiskLevel.HIGH) {
            return ApprovalRoute.DIRECTOR_REVIEW;
        }
        if (isEscalatedByRisk(amount, risk)) {
            return ApprovalRoute.DIRECTOR_REVIEW;
        }
        if (amount >= FINANCE_LIMIT) {
            return ApprovalRoute.FINANCE_REVIEW;
        }
        if (urgent) {
            return ApprovalRoute.FAST_TRACK;
        }
        return ApprovalRoute.STANDARD;
    }

    /** Вариант 5: MEDIUM-риск и сумма >= RISK_ESCALATION_PERCENT % от порога. */
    private boolean isEscalatedByRisk(long amount, RiskLevel risk) {
        long escalationLimit = FINANCE_LIMIT * RISK_ESCALATION_PERCENT / 100;
        return risk == RiskLevel.MEDIUM && amount >= escalationLimit;
    }

    private void validate(long amount, RiskLevel risk) {
        if (amount < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной: " + amount);
        }
        if (risk == null) {
            throw new IllegalArgumentException("Уровень риска не задан");
        }
    }
}
