package ru.checkdev.notification.telegram.action.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.checkdev.notification.service.UserTelegramService;
import ru.checkdev.notification.telegram.SessionTg;
import ru.checkdev.notification.telegram.service.TgAuthCallWebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class BindAccountActionTest {
    private static final String ERROR_MAIL = "error@exception.er";
    private static final Chat CHAT = new Chat(1L, "type");

    @Mock
    private TgAuthCallWebClient tgCall;
    @Mock
    private UserTelegramService userTelegramService;

    private SessionTg sessionTg;
    private Update update;
    private Message message;
    private BindAccountAction bindAccountAction;


    @BeforeEach
    void setUp() {
        sessionTg = new SessionTg();
        update = new Update();
        message = new Message();
        bindAccountAction = new BindAccountAction(sessionTg, tgCall, userTelegramService);
    }

    @Test
    void whenBindThenMessageAccountHasBound() {
        message.setChat(CHAT);
        update.setMessage(message);

        sessionTg.put(String.valueOf(CHAT.getId()), "email", "email@email.ru");
        sessionTg.put(String.valueOf(CHAT.getId()), "password", "password");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", 123);

        when(tgCall.doPost(anyString(), any())).thenReturn(Mono.just(resultMap));
        when(userTelegramService.save(any())).thenReturn(true);

        BotApiMethod botApiMethod = bindAccountAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;
        String actualMessage = sendMessage.getText();

        String expectMessage = "Ваш аккаунт CheckDev успешно привязан к данному аккаунту Telegram";

        assertThat(actualMessage).isEqualTo(expectMessage);
        verify(userTelegramService, times(1)).save(any());
    }

    @Test
    void whenExceptionAtBindingThenMessageServiceIsUnavailable() {
        message.setChat(CHAT);
        update.setMessage(message);

        sessionTg.put(String.valueOf(CHAT.getId()), "email", ERROR_MAIL);

        when(tgCall.doPost(anyString(), any())).thenThrow(new RuntimeException("service error"));

        String expect = String.format("Сервис недоступен, попробуйте позже%s%s", System.lineSeparator(), "/start");

        BotApiMethod botApiMethod = bindAccountAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;
        String actual = sendMessage.getText();

        assertThat(actual).isEqualTo(expect);
        verify(userTelegramService, never()).save(any());
    }
}