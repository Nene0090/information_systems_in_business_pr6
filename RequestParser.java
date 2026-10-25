/**
 * Разбор строки CSV с заявкой.
 * Формат строки: сумма,срочная,риск,персональные_данные  (например "600000,true,LOW,false").
 * Любая ошибка - IllegalArgumentException с понятным сообщением.
 */
public class RequestParser {

    public Request parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Пустая строка заявки");
        }
        String[] c = line.split(",", -1);
        if (c.length != 4) {
            throw new IllegalArgumentException("Ожидается 4 поля, получено " + c.length);
        }
        long amount = parseAmount(c[0].trim());
        boolean urgent = parseBoolean(c[1].trim(), "срочность");
        RiskLevel risk = parseRisk(c[2].trim());
        boolean personal = parseBoolean(c[3].trim(), "персональные данные");
        return new Request(amount, urgent, risk, personal);
    }

    private long parseAmount(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Сумма не задана");
        }
        long value;
        try {
            value = Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Сумма '" + s + "' не является целым числом");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной: " + value);
        }
        return value;
    }

    private boolean parseBoolean(String s, String field) {
        if (s.equals("true")) {
            return true;
        }
        if (s.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException("Поле '" + field + "': ожидается true или false, получено '" + s + "'");
    }

    private RiskLevel parseRisk(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Уровень риска не задан");
        }
        try {
            return RiskLevel.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный уровень риска '" + s + "' (допустимо LOW, MEDIUM, HIGH)");
        }
    }
}
