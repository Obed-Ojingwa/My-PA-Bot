import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.*

class PersonalAssistantBot : TelegramLongPollingBot() {

    private val chatIds = mutableSetOf<Long>()

    override fun getBotToken(): String {
        return bot_UserName.BotUser.botToken
    }

    override fun getBotUsername(): String {
        return bot_UserName.BotUser.botUserName
    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val messageText = update.message.text
            val chatId = update.message.chatId
            chatIds.add(chatId) // Store the chat ID for weekly notifications

            when (messageText.lowercase()) {
                "/start" -> sendMessage(chatId, "Hello! I am your personal assistant bot. What can I do for you. '/profession'")
                "/website" -> sendMessage(chatId, "Here is my website: [my website](https://myportfolio-obed.netlify.app/)")
                "/profession" -> sendMessage(chatId, "I'm Obed, an experienced Android developer with a strong command of Kotlin, Python, and GIS. My background includes creating user-friendly mobile applications and integrating spatial data effectively. I'm passionate about technology and continuously update my skills to provide innovative solutions. I believe my diverse skill set and problem-solving abilities make me a valuable addition to your team. Thank for taking your time, I'm available to work and communicate with you. Contact me '/contact' See my website '/website'")
                "/contact" -> sendMessage(chatId, "message me on Gmail: obedojingwa@gmail.com, WhatsApp: https://wa.me/+2348102544186  on Linkdin: https://www.linkedin.com/in/obed-ojingwa-94a73422a/  on Facebook:  https://web.facebook.com/chuks.odswillxxxx?_rdc=1&_rdr")
                else -> sendMessage(chatId, "Sorry, I didn't understand that command, here is a list of what I understand for now, '/profession' to see what I can offer, '/website', can you contact my owner by sending some key commands like '/contact'?.")
            }
        }
    }

    private fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = text
        message.enableMarkdown(true)

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    fun sendWeeklyNotification() {
        val notificationText = """
            Hi! What would you want me to do for you? Here are some options to contact me:
            - Gmail: Inbox me (obedojingwa@gmail.com)
            - WhatsApp: my WhatsApp (https://wa.me/+2348102544186)
        """.trimIndent()

        for (chatId in chatIds) {
            sendMessage(chatId, notificationText)
        }
    }
}

class NotificationJob : Job {
    override fun execute(context: JobExecutionContext?) {
        val bot = context?.jobDetail?.jobDataMap?.get("bot") as PersonalAssistantBot
        bot.sendWeeklyNotification()
    }
}

fun main() {
    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
    val bot = PersonalAssistantBot()

    try {
        botsApi.registerBot(bot)
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }

    // Schedule the weekly notification job
    val job = JobBuilder.newJob(NotificationJob::class.java)
        .withIdentity("notificationJob", "group1")
        .build()

    val trigger = TriggerBuilder.newTrigger()
        .withIdentity("notificationTrigger", "group1")
        .startNow()
        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInHours(168) // 168 hours = 1 week
            .repeatForever())
        .build()

    val scheduler = StdSchedulerFactory.getDefaultScheduler()
    scheduler.context.put("bot", bot)
    scheduler.start()
    scheduler.scheduleJob(job, trigger)
}
