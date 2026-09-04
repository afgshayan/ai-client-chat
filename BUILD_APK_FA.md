# راهنمای ساخت و گرفتن خروجی APK

این سند مرحله‌به‌مرحله توضیح می‌دهد که چطور پروژه‌ی «AI Client Chat» را باز کنید، اجرا کنید و در نهایت یک فایل APK (برای تست یا انتشار) از آن بگیرید.

> این پروژه یک اپ اندروید بومی با Kotlin/Jetpack Compose است. برای ساخت آن حتماً به **Android Studio** و **اتصال اینترنت** (برای دانلود Gradle، Android SDK و کتابخانه‌ها) نیاز دارید. گرفتن خروجی APK در محیط‌های بدون Android SDK (مثل یک سرور ابری ساده) ممکن نیست.

## ۱) پیش‌نیازها

1. نصب **Android Studio** (آخرین نسخه‌ی پایدار — Ladybug یا جدیدتر) از [developer.android.com/studio](https://developer.android.com/studio).
   - Android Studio به‌صورت پیش‌فرض JDK 17 را همراه خودش نصب می‌کند، نیازی به نصب جداگانه‌ی JDK نیست.
2. حداقل ۱۰ گیگابایت فضای خالی دیسک (برای Android SDK، Gradle cache و شبیه‌ساز).
3. یک کلید API از Anthropic برای استفاده‌ی واقعی از اپ (نه برای ساخت APK) — از [console.anthropic.com](https://console.anthropic.com) قابل دریافت است. این کلید داخل خود اپ و در صفحه‌ی خوش‌آمدگویی وارد می‌شود، نه در کد.

## ۲) باز کردن پروژه

1. پروژه را از مخزن گیت دریافت کنید (Clone) یا فولدر پروژه را دانلود کنید.
2. Android Studio را باز کنید → **Open** → پوشه‌ی ریشه‌ی پروژه (همان پوشه‌ای که فایل‌های `settings.gradle.kts` و `build.gradle.kts` در آن است) را انتخاب کنید.
3. صبر کنید تا **Gradle Sync** به‌صورت خودکار انجام شود (نوار پیشرفت پایین پنجره). اولین Sync ممکن است چند دقیقه طول بکشد چون کتابخانه‌ها دانلود می‌شوند.
4. اگر Android Studio پیام داد که «Android SDK Platform 35» یا «Build Tools» نصب نیست، روی لینک پیشنهادی (یا از مسیر **Tools → SDK Manager**) کلیک کنید و آن‌ها را نصب کنید.
5. اگر پیغام به‌روزرسانی نسخه‌ی کتابخانه‌ای (Kotlin/Gradle/AGP/KSP) دیدید، پیشنهاد خودکار Android Studio را بپذیرید؛ نسخه‌های استفاده‌شده در پروژه تازه هستند ولی ممکن است در زمان ساخت شما نسخه‌ی جدیدتری در دسترس باشد.

## ۳) اجرا برای تست (بدون نیاز به APK جداگانه)

اگر فقط می‌خواهید اپ را امتحان کنید:

1. یک دستگاه اندرویدی واقعی را با کابل USB وصل کنید (و حالت «Developer Options → USB debugging» را در گوشی فعال کنید) یا یک شبیه‌ساز (Emulator) از **Device Manager** در Android Studio بسازید (حداقل Android 8 / API 26).
2. روی دکمه‌ی سبز رنگ ▶ **Run** در بالای Android Studio کلیک کنید.
3. اپ نصب و اجرا می‌شود؛ کلید API خود را در صفحه‌ی اول وارد کنید تا گفتگو را شروع کنید.

## ۴) گرفتن APK نسخه‌ی Debug (سریع، فقط برای تست)

این خروجی امضای رسمی ندارد و فقط برای نصب دستی/تست مناسب است.

**از داخل Android Studio:**
منوی **Build → Build App Bundle(s) / APK(s) → Build APK(s)** را بزنید. بعد از پایان build، لینک «locate» در گوشه‌ی پایین‌سمت‌راست ظاهر می‌شود که مسیر فایل را نشان می‌دهد.

**از خط فرمان (ترمینال، داخل پوشه‌ی پروژه):**

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

فایل خروجی اینجا ساخته می‌شود:
`app/build/outputs/apk/debug/app-debug.apk`

## ۵) گرفتن APK نسخه‌ی Release (امضاشده، برای انتشار واقعی)

برای انتشار (حتی به‌صورت مستقیم بین دوستان یا در فروشگاه‌ها) باید اپ را با یک **کلید امضای دیجیتال (keystore)** امضا کنید.

### روش ساده: از طریق رابط گرافیکی Android Studio

1. منوی **Build → Generate Signed App Bundle / APK…** را باز کنید.
2. گزینه‌ی **APK** را انتخاب کرده و Next بزنید.
3. اگر تا به حال keystore نساخته‌اید، روی **Create new…** کلیک کنید:
   - مسیر ذخیره‌ی فایل keystore (مثلاً `ai-client-chat.jks`) را انتخاب کنید.
   - یک رمز عبور برای keystore و یک رمز عبور برای «key alias» تعیین کنید (این‌ها را جایی امن یادداشت کنید — بدون آن‌ها امکان به‌روزرسانی بعدی اپ منتشرشده وجود ندارد).
   - اطلاعات certificate (نام، سازمان، کشور و…) را پر کنید و اعتبار را روی مقداری بالا (مثلاً ۲۵ سال) بگذارید.
4. Next را بزنید، Build Variant را روی **release** بگذارید، تیک **V1 (Jar Signature)** و **V2 (Full APK Signature)** را فعال نگه دارید، و Finish را بزنید.
5. بعد از پایان ساخت، فایل امضاشده در `app/release/app-release.apk` (یا مسیری که خودتان انتخاب کردید) قرار می‌گیرد.

### روش خط فرمان (برای CI یا کاربران حرفه‌ای‌تر)

۱. ساخت keystore با ابزار `keytool` (همراه JDK می‌آید):

```bash
keytool -genkeypair -v -keystore ai-client-chat.jks -keyalg RSA -keysize 2048 -validity 9125 -alias ai_client_key
```

۲. قبل از build، این متغیرهای محیطی را ست کنید (پروژه از قبل برای خواندن آن‌ها در `app/build.gradle.kts` تنظیم شده است):

```bash
export RELEASE_KEYSTORE_PATH=/مسیر/کامل/ai-client-chat.jks
export RELEASE_KEYSTORE_PASSWORD="رمز-کیاستور"
export RELEASE_KEY_ALIAS="ai_client_key"
export RELEASE_KEY_PASSWORD="رمز-کلید"
```

۳. ساخت نسخه‌ی release:

```bash
./gradlew assembleRelease
```

فایل خروجی: `app/build/outputs/apk/release/app-release.apk`

> ⚠️ هرگز فایل keystore یا رمزهای آن را داخل گیت کامیت نکنید. این پروژه در `.gitignore` از قبل پسوندهای `*.jks` و `*.keystore` را نادیده می‌گیرد.

## ۶) نصب فایل APK روی گوشی

1. فایل `.apk` را به گوشی اندرویدی منتقل کنید (از طریق کابل، لینک دانلود خصوصی یا هر روش دیگر).
2. روی فایل در File Manager گوشی بزنید. اگر پیام «نصب از منابع ناشناس مسدود شده» ظاهر شد، طبق راهنمای گوشی به برنامه‌ی موردنظر (مثلاً «Files» یا مرورگر) اجازه‌ی نصب از منابع ناشناس بدهید (در Settings → Apps → دسترسی خاص → Install unknown apps).
3. نصب را تأیید کنید و اپ را باز کنید.

## ۷) اشکال‌های رایج

| مشکل | راه‌حل |
|---|---|
| «SDK location not found» هنگام build از خط فرمان | یک‌بار پروژه را در Android Studio باز کنید تا فایل `local.properties` با مسیر SDK به‌صورت خودکار ساخته شود، یا خودتان بسازید: `sdk.dir=/مسیر/Android/Sdk` |
| خطای نسخه‌ی Gradle/AGP/Kotlin هنگام Sync | پیشنهاد به‌روزرسانی خودکار Android Studio را بپذیرید؛ یا نسخه‌ها را در `build.gradle.kts` و `gradle/wrapper/gradle-wrapper.properties` هماهنگ با آخرین نسخه‌های پایدار کنید |
| فضای کم حافظه هنگام build (`OutOfMemoryError`) | مقدار `org.gradle.jvmargs` در `gradle.properties` را افزایش دهید (مثلاً `-Xmx4096m`) |
| اپ روی گوشی باز نمی‌شود / بلافاصله می‌بندد | یک نسخه‌ی Debug بسازید و از **Logcat** در Android Studio خطای دقیق را ببینید |
| پاسخی از هوش مصنوعی دریافت نمی‌شود | مطمئن شوید کلید API معتبر است و در تنظیمات اپ درست وارد شده؛ اتصال اینترنت گوشی را بررسی کنید |

موفق باشید.
