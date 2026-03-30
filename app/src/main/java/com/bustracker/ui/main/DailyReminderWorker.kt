package com.karroh.bussathi.util // Make sure this matches your actual package name!

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.karroh.bussathi.R
import com.karroh.bussathi.ui.splash.IntroAnimationActivity // Make sure this points to the screen you want to open!

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    // Our list of 20 rotating notifications (1 original + 19 new)
    private val notifications = listOf(
        Pair("कोई भी ट्रिप मिस न करें! / ایک اور دن، ایک اور ٹرپ", "कृपया अपनी दैनिक ट्रिप शुरू करना याद रखें। इसके बिना, आप अपना शेड्यूल भूल सकते हैं। / براہ کرم اپنے روزانہ کے ٹرپس شروع کرنا یاد رکھیں۔ اس کے بغیر آپ اپنا شیڈول بھول سکتے ہیں۔"),
        Pair("आज के दिन के लिए तैयार हैं? / دن کے لیے تیار ہیں؟", "अभी अपना रूट ट्रैक करना शुरू करें! / ابھی اپنا روٹ ٹریک کرنا شروع کریں!"),
        Pair("आपका रूट महत्वपूर्ण है! / آپ کا روٹ اہم ہے!", "अपनी ट्रिप हिस्ट्री को अपडेट रखें। / اپنی ٹرپ ہسٹری کو اپ ڈیٹ رکھیں۔"),
        Pair("सुरक्षित ड्राइव करें! / احتیاط سے چلائیں!", "इंजन शुरू करने से पहले बस साथी ऑन करें। / انجن شروع کرنے سے پہلے بس ساتھی آن کریں۔"),
        Pair("एक नया दिन, एक नई ट्रिप / ایک اور دن، ایک اور ٹرپ", "आइए आज का सफर रिकॉर्ड करें! / آئیے آج کا سفر ریکارڈ کریں!"),
        Pair("टॉप ड्राइवर अलर्ट! / ٹاپ ڈرائیور الرٹ!", "लगातार ट्रैकिंग आपको एक प्रो बनाती है। / مستقل ٹریکنگ آپ کو ماہر بناتی ہے۔"),
        Pair("सुप्रभात! / صبح بخیر!", "सड़क पर निकलने का समय आ गया है। अपनी ट्रिप शुरू करें। / سڑک پر نکلنے کا وقت۔ اپنا ٹرپ شروع کریں۔"),
        Pair("यात्रियों को आप पर भरोसा है / مسافروں کا آپ پر انحصار ہے", "सब कुछ समय पर रखने के लिए अपनी ट्रिप शुरू करें। / سب کچھ شیڈول پر رکھنے کے لیے ٹرپ شروع کریں۔"),
        Pair("बेहतरीन काम जारी रखें / اچھا کام جاری رکھیں", "अपने सुबह के रूट को रिकॉर्ड करना न भूलें। / اپنے صبح کے روٹ کو ریکارڈ کرنا نہ بھولیں۔"),
        Pair("अपनी सफलता को ट्रैक करें / اپنی کامیابی کو ٹریک کریں", "हर ट्रिप मायने रखती है। ट्रैकिंग शुरू करें! / ہر ٹرپ اہم ہے۔ ٹریکنگ شروع کریں!"),
        Pair("बस साथी तैयार है / بس ساتھی تیار ہے", "शुरू करते समय बस 'स्टार्ट ट्रिप' पर टैप करें। / شروع کرتے وقت بس 'ٹرپ شروع کریں' پر ٹیپ کریں۔"),
        Pair("सुरक्षित सफर / بخیریت", "सुरक्षा और रिकॉर्ड के लिए अपनी ट्रिप रिकॉर्ड करें। / حفاظت اور ریکارڈ کے لیے اپنے ٹرپس ریکارڈ کریں۔"),
        Pair("ड्राइव करने के लिए तैयार हैं? / چلانے کے لیے تیار ہیں؟", "आपका डैशबोर्ड आपका इंतज़ार कर रहा है। / آپ کا ڈیش بورڈ آپ کا انتظار کر رہا ہے۔"),
        Pair("दैनिक रिमाइंडर / روزانہ کی یاد دہانی", "कृपया सुनिश्चित करें कि आपका GPS ऑन है और ट्रिप शुरू करें। / براہ کرم یقینی بنائیں کہ آپ کا GPS آن ہے اور ٹرپ شروع کریں۔"),
        Pair("चलो चलें! / چلو چلتے ہیں!", "आपका अगला सफर शुरू होने वाला है। / آپ کا اگلا سفر شروع ہونے والا ہے۔"),
        Pair("प्रोफेशनल ट्रैकिंग / پروفیشنل ٹریکنگ", "बस साथी के साथ आसानी से अपनी दूरी लॉग करें। / بس ساتھی کے ساتھ آسانی سے اپنا فاصلہ لاگ کریں۔"),
        Pair("आपकी यात्रा शानदार हो! / آپ کا سفر شاندار ہو!", "हम हर मील पर आपके साथ हैं। / ہم ہر میل پر آپ کے ساتھ ہیں۔"),
        Pair("रूट मास्टर / روٹ ماسٹر", "अपनी ट्रिप ट्रैक करके अपनी पाबंदी दिखाएं। / اپنا ٹرپ ٹریک کر کے اپنی پابندی دکھائیں۔"),
        Pair("इंजन ऑन, ऐप ऑन / انجن آن، ایپ آن", "सबसे पहले बस साथी शुरू करने की आदत बनाएं! / بس ساتھی کو پہلے شروع کرنے کی عادت بنائیں!"),
    )

    override fun doWork(): Result {
        sendRotatingNotification()
        return Result.success()
    }

    private fun sendRotatingNotification() {
        // 1. Get the SharedPreferences to remember which message we sent yesterday
        val prefs = context.getSharedPreferences("BusSathiPrefs", Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt("daily_notification_index", 0)

        // 2. Fetch the notification text for today
        val notificationData = notifications[currentIndex]

        // 3. Calculate the index for tomorrow (If we hit 20, it loops back to 0!)
        val nextIndex = (currentIndex + 1) % notifications.size
        prefs.edit().putInt("daily_notification_index", nextIndex).apply()

        // 4. Send the notification to the phone
        showNotification(notificationData.first, notificationData.second)
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_reminder_channel"

        // Required for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Morning Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // What happens when they tap the notification (Opens the Splash Screen)
        val intent = Intent(context, IntroAnimationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the actual notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // *Note: See my comment below!*
            .setContentTitle(title)
            .setContentText(message)
            // BigTextStyle ensures the long bilingual message doesn't get cut off!
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}