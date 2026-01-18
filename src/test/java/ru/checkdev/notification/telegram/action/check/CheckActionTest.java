package ru.checkdev.notification.telegram.action.check;

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
import ru.checkdev.notification.domain.Profile;
import ru.checkdev.notification.domain.UserTelegram;
import ru.checkdev.notification.service.UserTelegramService;
import ru.checkdev.notification.telegram.SessionTg;
import ru.checkdev.notification.telegram.service.TgAuthCallWebClient;
import reactor.core.publisher.Mono;

import java.util.Calendar;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class CheckActionTest {

    private static final Chat CHAT = new Chat(1L, "type");

    @Mock
    private UserTelegramService userTelegramService;
    @Mock
    private TgAuthCallWebClient tgCall;

    private SessionTg sessionTg;
    private CheckAction checkAction;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        sessionTg = new SessionTg();
        checkAction = new CheckAction(sessionTg, tgCall, userTelegramService);
        update = new Update();
        message = new Message();
    }

    @Test
    void whenNotChatId() {
        message.setChat(CHAT);
        update.setMessage(message);

        when(userTelegramService.findByChatId(CHAT.getId())).thenReturn(Optional.empty());

        BotApiMethod<Message> botApiMethod = checkAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;

        String text = "Данный аккаунт Telegram на сайте не зарегистрирован";
        assertThat(text).isEqualTo(sendMessage.getText());
        verify(userTelegramService, times(1)).findByChatId(CHAT.getId());
    }

    @Test
    void whenHandleChatIdIsPresentThenReturnMessage() {
        message.setChat(CHAT);
        update.setMessage(message);

        UserTelegram userTelegram = new UserTelegram(0, 1, CHAT.getId(), false);
        when(userTelegramService.findByChatId(CHAT.getId())).thenReturn(Optional.of(userTelegram));

        Profile profile = new Profile(1, "FakeName", "FakeEmail", "pwd", true, Calendar.getInstance());
        when(tgCall.doGet("/profiles/tg/" + userTelegram.getUserId())).thenReturn(Mono.just(profile));

        BotApiMethod<Message> botApiMethod = checkAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;

        String ls = System.lineSeparator();
        String expect = "Имя:" + ls
                + "FakeName" + ls
                + "Email:" + ls
                + "FakeEmail" + ls;

        assertThat(expect).isEqualTo(sendMessage.getText());
        verify(userTelegramService, times(1)).findByChatId(CHAT.getId());
        verify(tgCall, times(1)).doGet("/profiles/tg/" + userTelegram.getUserId());
    }

    @Test
    void whenHandleChatIdIsPresentThenReturnServiceError() {
        message.setChat(CHAT);
        update.setMessage(message);

        UserTelegram userTelegram = new UserTelegram(0, 1, CHAT.getId(), false);
        when(userTelegramService.findByChatId(CHAT.getId())).thenReturn(Optional.of(userTelegram));

        when(tgCall.doGet("/profiles/tg/" + userTelegram.getUserId()))
                .thenThrow(new RuntimeException("service error"));

        BotApiMethod<Message> botApiMethod = checkAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;

        String text = "Сервис не доступен попробуйте позже";
        assertThat(text).isEqualTo(sendMessage.getText());
        verify(userTelegramService, times(1)).findByChatId(CHAT.getId());
        verify(tgCall, times(1)).doGet("/profiles/tg/" + userTelegram.getUserId());
    }
}