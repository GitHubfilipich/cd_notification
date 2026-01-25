package ru.checkdev.notification.telegram.action.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class BindAskPasswordActionTest {

    private static final Chat CHAT = new Chat(1L, "type");

    private BindAskPasswordAction bindAskPasswordAction;
    private Message message;
    private Update update;

    @BeforeEach
    void setUp() {
        bindAskPasswordAction = new BindAskPasswordAction();
        message = new Message();
        update = new Update();
    }

    @Test
    void whenHandleThenReturnAskPasswordMessage() {
        message.setChat(CHAT);
        update.setMessage(message);

        Optional<BotApiMethod> result = bindAskPasswordAction.handle(update);

        assertThat(result).isPresent();
        BotApiMethod method = result.get();
        assertThat(method).isInstanceOf(SendMessage.class);

        SendMessage sendMessage = (SendMessage) method;
        assertThat(sendMessage.getChatId()).isEqualTo(CHAT.getId().toString());
        assertThat(sendMessage.getText()).isEqualTo("Введите пароль:");
    }
}