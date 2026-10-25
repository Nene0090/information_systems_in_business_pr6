import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Запуск:
 *   java Main                      - обработать data/sample.csv
 *   java Main путь/к/файлу.csv     - обработать указанный файл (id,request,expected)
 *   java Main --line "600000,true,LOW,false"
 * Формат файла: id;сумма;срочная;риск;персональные_данные;ожидаемый_маршрут (разделитель ';').
 */
public class Main {

    public static void main(String[] args) throws IOException {
        RequestParser parser = new RequestParser();
        ApprovalRouter router = new ApprovalRouter();
        if (args.length == 2 && args[0].equals("--line")) {
            System.out.println(describe(parser, router, args[1]));
            return;
        }
        String path = args.length == 1 ? args[0] : "data/sample.csv";
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) { // строка 0 - заголовок
            String[] c = lines.get(i).split(";", -1);
            if (c.length < 5) {
                continue;
            }
            String request = c[1] + "," + c[2] + "," + c[3] + "," + c[4];
            System.out.println(c[0] + ": " + describe(parser, router, request));
        }
    }

    private static String describe(RequestParser parser, ApprovalRouter router, String line) {
        try {
            Request r = parser.parse(line);
            ApprovalRoute route = router.route(r.amount(), r.urgent(), r.risk(), r.containsPersonalData());
            return route.name();
        } catch (IllegalArgumentException e) {
            return "ОШИБКА: " + e.getMessage();
        }
    }
}
