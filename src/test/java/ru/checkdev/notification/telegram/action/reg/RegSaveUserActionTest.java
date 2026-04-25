package ru.checkdev.notification.telegram.action.reg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import reactor.core.publisher.Mono;
import ru.checkdev.notification.service.EurekaUriProvider;
import ru.checkdev.notification.service.UserTelegramService;
import ru.checkdev.notification.telegram.SessionTg;
import ru.checkdev.notification.telegram.service.TgAuthCallWebClient;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Dmitry Stepanov, user Dmitry
 * @since 27.11.2023
 */

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@Disabled
class RegSaveUserActionTest {
    private static final String ERROR_MAIL = "error@exception.er";
    private static final Chat CHAT = new Chat(1L, "type");
    private static final String URL_SITE_AUTH = "www";

    @Mock
    private DiscoveryClient discoveryClient;
    @Mock
    private UserTelegramService userTelegramService;
    @Mock
    private TgAuthCallWebClient tgCall;

    private EurekaUriProvider uriProvider;
    private SessionTg sessionTg;
    private Message message;
    private Update update;
    private RegSaveUserAction regSaveUserAction;

    @BeforeEach
    void setUp() {
        uriProvider = new EurekaUriProvider(discoveryClient);
        sessionTg = new SessionTg();
        regSaveUserAction =
                new RegSaveUserAction(sessionTg,
                        tgCall, userTelegramService, uriProvider, URL_SITE_AUTH);
        message = new Message();
        update = new Update();
    }

    @Test
    void whenSaveActionNotEmailThenReturnMessageRepeat() {
        message.setChat(CHAT);
        update.setMessage(message);
        String ls = System.lineSeparator();
        String text = "Пройдите регистрацию заново" + ls + "/new";

        BotApiMethod botApiMethod = regSaveUserAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;

        assertThat(text).isEqualTo(sendMessage.getText());
        verify(userTelegramService, never()).save(any());
    }

    @Test
    void whenCallBackThenOk() throws URISyntaxException {
        message.setChat(CHAT);
        update.setMessage(message);
        String email = "email@email.ru";
        String name = "nameUser";
        sessionTg.put(String.valueOf(CHAT.getId()), "email", email);
        sessionTg.put(String.valueOf(CHAT.getId()), "name", name);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", 123);
        when(tgCall.doPost(anyString(), any())).thenReturn(Mono.just(resultMap));

        BotApiMethod botApiMethod = regSaveUserAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;
        String actual = sendMessage.getText();
        String ls = System.lineSeparator();
        String passwordInMessage = getPassInMessage(actual, URL_SITE_AUTH);
        String expect = new StringBuilder().append("Вы зарегистрированы: ").append(ls)
                .append("Имя: ").append(name).append(ls)
                .append("Email: ").append(email).append(ls)
                .append("Пароль : ").append(passwordInMessage).append(ls)
                .append(URL_SITE_AUTH).toString();

        assertThat(actual).isEqualTo(expect);
        verify(userTelegramService, times(1)).save(any());
    }

    @Test
    void whenCallBackThenErrorService() {
        message.setChat(CHAT);
        update.setMessage(message);
        String name = "nameUser";
        sessionTg.put(String.valueOf(CHAT.getId()), "email", ERROR_MAIL);
        sessionTg.put(String.valueOf(CHAT.getId()), "name", name);

        when(tgCall.doPost(anyString(), any())).thenThrow(new RuntimeException("service error"));

        String ls = System.lineSeparator();
        String expect = String.format("Сервис не доступен попробуйте позже%s%s", ls, "/start");

        BotApiMethod botApiMethod = regSaveUserAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;
        String actual = sendMessage.getText();

        assertThat(actual).isEqualTo(expect);
        verify(userTelegramService, never()).save(any());
    }

    private String getPassInMessage(String textMessage, String urlSiteAuth) {
        String startDelimiter = "Пароль : ";
        int startPassIndex = textMessage.indexOf(startDelimiter) + startDelimiter.length();
        int endPassIndex = textMessage.lastIndexOf(System.lineSeparator() + urlSiteAuth);
        return textMessage.substring(startPassIndex, endPassIndex);
    }
}