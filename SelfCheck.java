import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Самопроверка без внешних библиотек. Код возврата 1, если хотя бы одна проверка не прошла.
 * Запуск: java SelfCheck [путь к sample.csv]
 */
public class SelfCheck {

    private static final ApprovalRouter ROUTER = new ApprovalRouter();
    private static final RequestParser PARSER = new RequestParser();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("--- Таблица решений ---");
        Object[][] table = {
            {100000L, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD, "обычная заявка"},
            {100000L, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK, "срочная, сумма меньше порога"},
            {100000L, false, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW, "персональные данные"},
            {100000L, true, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW, "срочность не перекрывает персональные данные"},
            {100000L, false, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW, "риск HIGH"},
            {100000L, true, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW, "срочность не перекрывает риск HIGH"},
            {600000L, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW, "сумма не меньше порога"},
            {600000L, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW, "срочная, но сумма не меньше порога"},
            {600000L, false, RiskLevel.HIGH, false, ApprovalRoute.DIRECTOR_REVIEW, "риск HIGH строже суммы"},
            {600000L, false, RiskLevel.LOW, true, ApprovalRoute.SECURITY_REVIEW, "персональные данные при большой сумме"},
            {600000L, true, RiskLevel.HIGH, true, ApprovalRoute.SECURITY_REVIEW, "все условия сразу: побеждает самое строгое"},
            {100000L, false, RiskLevel.MEDIUM, false, ApprovalRoute.STANDARD, "вариант 5: MEDIUM, сумма ниже 150 000"},
            {200000L, false, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW, "вариант 5: MEDIUM, сумма от 150 000"},
            {600000L, false, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW, "вариант 5: риск MEDIUM перекрывает финансовую проверку"},
            {200000L, true, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW, "вариант 5: риск MEDIUM перекрывает срочность"},
            {0L, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD, "нулевая сумма"},
        };
        for (int i = 0; i < table.length; i++) {
            Object[] t = table[i];
            route("T" + (i + 1) + " " + t[5], (Long) t[0], (Boolean) t[1], (RiskLevel) t[2],
                    (Boolean) t[3], (ApprovalRoute) t[4]);
        }

        System.out.println("--- Границы суммы ---");
        route("499999, не срочная", 499999L, false, RiskLevel.LOW, false, ApprovalRoute.STANDARD);
        route("500000, не срочная", 500000L, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        route("500001, не срочная", 500001L, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        route("499999, срочная", 499999L, true, RiskLevel.LOW, false, ApprovalRoute.FAST_TRACK);
        route("500000, срочная", 500000L, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        route("500001, срочная", 500001L, true, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);
        route("MEDIUM, 149999", 149999L, false, RiskLevel.MEDIUM, false, ApprovalRoute.STANDARD);
        route("MEDIUM, 150000", 150000L, false, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW);
        route("MEDIUM, 150001", 150001L, false, RiskLevel.MEDIUM, false, ApprovalRoute.DIRECTOR_REVIEW);
        route("Long.MAX_VALUE, LOW", Long.MAX_VALUE, false, RiskLevel.LOW, false, ApprovalRoute.FINANCE_REVIEW);

        System.out.println("--- Ошибочные входные данные ---");
        badRouter("отрицательная сумма в route", -1L, RiskLevel.LOW);
        badRouter("risk == null в route", 100L, null);
        badLine("пустая строка", "", "Пустая строка");
        badLine("пустая сумма", ",false,LOW,false", "Сумма не задана");
        badLine("сумма не число", "abc,false,LOW,false", "не является целым числом");
        badLine("отрицательная сумма", "-5,false,LOW,false", "отрицательной");
        badLine("риск не задан", "100,false,,false", "Уровень риска не задан");
        badLine("неизвестный риск", "100,false,EXTREME,false", "Неизвестный уровень риска");
        badLine("неверный boolean", "100,yes,LOW,false", "true или false");
        badLine("не 4 поля", "100,false,LOW", "Ожидается 4 поля");
        badLine("null вместо строки", null, "Пустая строка");

        System.out.println("--- Файл sample.csv ---");
        checkSampleCsv(args.length == 1 ? args[0] : "data/sample.csv");

        System.out.println();
        System.out.println("Итого: пройдено " + passed + ", не пройдено " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void route(String name, long amount, boolean urgent, RiskLevel risk,
                              boolean personal, ApprovalRoute expected) {
        ApprovalRoute actual = ROUTER.route(amount, urgent, risk, personal);
        check(name + " -> " + expected + (actual == expected ? "" : " (получено " + actual + ")"), actual == expected);
    }

    private static void badRouter(String name, long amount, RiskLevel risk) {
        try {
            ROUTER.route(amount, false, risk, false);
            check(name + " (ошибка не возникла)", false);
        } catch (IllegalArgumentException e) {
            check(name, true);
        }
    }

    private static void badLine(String name, String line, String messagePart) {
        try {
            PARSER.parse(line);
            check(name + " (ошибка не возникла)", false);
        } catch (IllegalArgumentException e) {
            check(name, e.getMessage().contains(messagePart));
        }
    }

    private static void checkSampleCsv(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) {
            String[] c = lines.get(i).split(";", -1);
            String expected = c[5];
            String actual;
            try {
                Request r = PARSER.parse(c[1] + "," + c[2] + "," + c[3] + "," + c[4]);
                actual = ROUTER.route(r.amount(), r.urgent(), r.risk(), r.containsPersonalData()).name();
            } catch (IllegalArgumentException e) {
                actual = "ERROR";
            }
            check("CSV " + c[0] + " (ожидалось " + expected + ", получено " + actual + ")", expected.equals(actual));
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
