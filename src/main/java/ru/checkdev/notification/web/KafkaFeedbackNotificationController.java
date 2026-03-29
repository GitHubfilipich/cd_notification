package ru.checkdev.notification.web;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.checkdev.notification.dto.FeedbackNotificationDTO;
import ru.checkdev.notification.service.NotificationMessagesService;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaFeedbackNotificationController {
    private final NotificationMessagesService notificationMessagesService;

    @KafkaListener(topics = "job4j_feedback_notification", groupId = "group-id")
    public void receiveFeedbackNotification(FeedbackNotificationDTO feedbackNotification) {
        notificationMessagesService.sendFeedbackNotification(feedbackNotification);
    }
}
