package ru.checkdev.notification.telegram.action.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.checkdev.notification.domain.UserTelegram;
import ru.checkdev.notification.service.UserTelegramService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnbindAccountActionTest {

    private static final Chat CHAT = new Chat(1L, "type");

    @Mock
    private UserTelegramService userTelegramService;

    private UnbindAccountAction unbindAccountAction;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        unbindAccountAction = new UnbindAccountAction(userTelegramService);
        update = new Update();
        message = new Message();
    }

    @Test
    void whenUnbindWithUserTelegramThenOk() {
        message.setChat(CHAT);
        update.setMessage(message);
        UserTelegram userTelegram = new UserTelegram(0, 0, CHAT.getId(), false);
        when(userTelegramService.findByChatId(CHAT.getId())).thenReturn(Optional.of(userTelegram));

        BotApiMethod botApiMethod = unbindAccountAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;
        String actualMessage = sendMessage.getText();
        String expectMessage = "Ваш аккаунт CheckDev отвязан от текущего аккаунта Telegram";

        assertThat(actualMessage).isEqualTo(expectMessage);
        verify(userTelegramService, times(1)).findByChatId(CHAT.getId());
        verify(userTelegramService, times(1)).delete(userTelegram);
    }

    @Test
    void whenUnbindWithoutUserTelegramThenMessageAccountIsNotBind() {
        message.setChat(CHAT);
        update.setMessage(message);
        when(userTelegramService.findByChatId(CHAT.getId())).thenReturn(Optional.empty());

        BotApiMethod botApiMethod = unbindAccountAction.handle(update).get();
        SendMessage sendMessage = (SendMessage) botApiMethod;
        String actualMessage = sendMessage.getText();
        String expectMessage = "К данному аккаунту телеграм не привязан аккаунт CheckDev";

        assertThat(actualMessage).isEqualTo(expectMessage);
        verify(userTelegramService, times(1)).findByChatId(CHAT.getId());
        verify(userTelegramService, never()).delete(any());
    }

}