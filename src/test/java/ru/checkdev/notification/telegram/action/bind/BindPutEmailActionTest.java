package ru.checkdev.notification.telegram.action.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.checkdev.notification.telegram.SessionTg;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BindPutEmailAction class.
 */
@ExtendWith(MockitoExtension.class)
class BindPutEmailActionTest {

    private static final Chat CHAT = new Chat(1L, "type");

    @Mock
    private SessionTg sessionTg;

    private BindPutEmailAction bindPutEmailAction;
    private Message message;
    private Update update;

    @BeforeEach
    void setUp() {
        bindPutEmailAction = new BindPutEmailAction(sessionTg);
        message = new Message();
        update = new Update();
    }

    @Test
    void whenPutEmailThenSessionStoredAndReturnEmptyOptional() {
        var email = "user@example.com";
        message.setChat(CHAT);
        message.setText(email);
        update.setMessage(message);

        Optional<BotApiMethod> result = bindPutEmailAction.handle(update);

        assertThat(result).isEmpty();
        verify(sessionTg, times(1)).put(String.valueOf(CHAT.getId()), "email", email);
    }
}