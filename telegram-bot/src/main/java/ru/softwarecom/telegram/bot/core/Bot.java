package ru.softwarecom.telegram.bot.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.softwarecom.telegram.bot.config.BotConfig;
import ru.softwarecom.telegram.bot.config.RegistrationConfig;
import ru.softwarecom.telegram.bot.model.RegistrationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final RegistrationConfig registrationConfig;

    private String phoneNumber;

    @Autowired
    public Bot(BotConfig botConfig, RegistrationConfig registrationConfig) {
        this.botConfig = botConfig;
        this.registrationConfig = registrationConfig;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {

            String message = "";
            String chatId = update.getMessage().getChatId().toString();

            SendMessage sm = new SendMessage();
            sm.setChatId(chatId);
            sm.setReplyMarkup(null);

            String text = update.getMessage().getText();
            if (text != null && !text.startsWith("/start")) {

                if (Pattern.matches("\\d{4}", text)) {
                    log.debug("Registration code: {}", text);
                    OkHttpClient client = new OkHttpClient();

                    String url = registrationConfig.getUrl() + "/" + phoneNumber + "/" + text;
                    log.debug("URL for calling php api: {}", url);

                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .build();
                    Call call = client.newCall(request);
                    Response response = call.execute();
                    String body = response.peekBody(Long.MAX_VALUE).string();
                    if (response.code() == 200) {

                        ObjectMapper mapper = new ObjectMapper();
                        RegistrationResponse registrationResponse = mapper.readValue(body, RegistrationResponse.class);
                        if (Objects.isNull(registrationResponse.getError()) || registrationResponse.getError().isEmpty()) {

                            url = registrationResponse.getUrl() + "?username=" + registrationResponse.getParams().getUsername() +
                                    "&password=" + registrationResponse.getParams().getPassword();

                            log.debug("url for router: {}", url);

                            message = "Регистрация завершена! Для продолжения нажмите кнопку \"Поехали!\"";
                            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                            List<InlineKeyboardButton> rowInline = new ArrayList<>();
                            InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                            keyboardButton.setText("Поехали!");
                            keyboardButton.setUrl(url);
                            rowInline.add(keyboardButton);
                            rowsInline.add(rowInline);
                            markupInline.setKeyboard(rowsInline);
                            sm.setReplyMarkup(markupInline);
                        } else {
                            log.debug("Error processing registration code: {}", registrationResponse.getError());
                            message = "Ошибка обработки кода: " + registrationResponse.getError();
                        }
                    } else {
                        log.debug("Response code: {}", response.code());
                        log.debug("Response body: {}", body);
                        message = "Ошибка обработки кода. Попробуйте ещё раз.";
                    }
                } else {
                    log.debug("Input text: {}", text);
                    message = "Ошибка ввода кода. Попробуйте её раз.";
                }
            } else {
                if (!Objects.isNull(update.getMessage().getContact())) {
                    phoneNumber = update.getMessage().getContact().getPhoneNumber().replace("+", "");
                    if (!Pattern.matches("\\d{11}", phoneNumber)) {
                        log.debug("Phone number is empty");
                        message = "При авторизации через телеграм произошла ошибка: не удалось получить номер телефона абонента. \n" +
                                "Попробуйте позже или пройдите авторизацию по звонку.";
                    } else {
                        log.debug("Phone number: {}", phoneNumber);
                        log.debug("Query registration code.");
                        message = "Введите 4-х значный код";
                    }
                } else {
                    log.debug("Start registration. Query phone number.");
                    message = "Для продолжения регистрации нажмите кнопку \"Мой номер телефона\"";
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    sm.setReplyMarkup(replyKeyboardMarkup);
                    replyKeyboardMarkup.setSelective(true);
                    replyKeyboardMarkup.setResizeKeyboard(true);
                    replyKeyboardMarkup.setOneTimeKeyboard(true);

                    List<KeyboardRow> keyboard = new ArrayList<>();
                    KeyboardRow keyboardFirstRow = new KeyboardRow();

                    KeyboardButton keyboardButton = new KeyboardButton();
                    keyboardButton.setText("Мой номер телефона");
                    keyboardButton.setRequestContact(true);
                    keyboardFirstRow.add(keyboardButton);

                    keyboard.add(keyboardFirstRow);

                    replyKeyboardMarkup.setKeyboard(keyboard);
                }
            }

            try {
                sm.setText(message);
                execute(sm);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
