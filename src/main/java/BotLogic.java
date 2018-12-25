
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.logging.Level;
import java.util.stream.IntStream;

import static org.telegram.telegrambots.meta.logging.BotLogger.log;

public class BotLogic extends TelegramLongPollingBot {
    static String[] TitleExceptions={};//возможно будет заполняться для ВСЕХ пользователь,нужно протестировать
    static String[] TagExceptions={};// ----//----
    private static int flag=0;

    @Override
    public void onUpdateReceived(Update update) {
        String message = update.getMessage().getText();
        System.out.println(message);
        if (message.equals("/start")) {
            sendMsg(update.getMessage().getChatId().toString(), "Привет! это YouTrendsBot!\n" +
                    "Список команд:\n"+
                    "/TitleExceptions (задать каналы-исключения)\n"+
                    "/TagExceptions (задать теги-исключения)\n"+
                    "/getVideoList");
        }
        if (message.equals("/TitleExceptions")) {
            sendMsg(update.getMessage().getChatId().toString(), "Перечислите каналы, которые вы хотите исключить, через запятую:");
            flag=1;
        }
        if (message.equals("/TagExceptions")) {
            sendMsg(update.getMessage().getChatId().toString(), "Перечислите теги, которые вы хотите исключить, через запятую:");
            flag=2;
        }
        if (message.equals("/getVideoList")) {
            sendMsg(update.getMessage().getChatId().toString(), "Подготавливаю список...");
            try {
                //System.out.println(Arrays.deepToString(Main.getVideoList()));
                Object[][] arr=YouTubeLogic.getVideoList();
                IntStream.range(0, arr.length).forEach(i -> {
                    sendMsg(update.getMessage().getChatId().toString(), i + 1 + ") " + String.valueOf(arr[i][0]));
                    sendMsg(update.getMessage().getChatId().toString(), "Количество просмотров: " + arr[i][1]);
                    sendMsg(update.getMessage().getChatId().toString(), "Ссылка: " + arr[i][2]);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (flag==1){
            TitleExceptions=message.replaceAll("\\s", "").split(",");
            flag=0;
        }
        else if (flag==2){
            TagExceptions=message.replaceAll("\\s", "").split(",");
            flag=0;
        }
    }

    private synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log(Level.SEVERE, "Exception: ", e.toString());
        }
    }

    @Override
    public String getBotUsername() {
        return "YouTrends_bot";
    }


    @Override
    public String getBotToken() {
        return "609308218:AAG7gM2C7HvNF4qS6vOkjU-LSX9uoJgI4DM";
    }
}