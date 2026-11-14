# AIStudy Companion (Android)

`AiStudyBot.exe` fayli tarkibi asosida Android uchun noldan yozilgan xavfsiz **kurs holatini ko'rish** ilovasi.

## Ishlaydigan imkoniyatlar

- Android 7.0+ uchun mahalliy Java ilovasi (internet kerak).
- 5 tagacha akkaunt va akkauntga 30 tagacha kurs raqamini kiritish.
- JWT formati nazorati va (tekshirilmagan) foydalanuvchi nomini token payloadidan ko'rish.
- AIStudy serverining `GetPrivateCourseContentTree` endpointidan kurs/darslar ma'lumotini **GET** orqali olish.
- Kurs bo'yicha tugallangan va qolgan darslarni server qaytargan `status` hamda `finishLessonCount` asosida ko'rsatish.
- Tekshirish / to'xtatish tugmalari, jurnal, AIStudy rasmiy saytini ochish.
- Token *diskka yozilmaydi*, reklama/telemetriya yo'q, HTTPS sertifikat tekshiruvi o'chirilmaydi.

## Asl .exe dan muhim farq

Bu ilova **video ko'rilgandek qilib soxta foiz yubormaydi**, **test savollariga avtomatik javob bermaydi**, platformaning baholash tizimini aylanib o'tmaydi. O'zlashtirish holatini faqat o'qiydi. Ilova AIStudy rasmiy mahsuloti emas.

## APK yaratish (Android Studio)

1. Kompyuterga **Android Studio** o'rnating (barqaror yangi versiya).
2. Zipdan `AIStudy_Android` papkasini chiqaring.
3. Android Studio > **Open** > shu papkani tanlang.
4. Agar Gradle o'rnatilmagan bo'lsa, Android Studio orqali Gradle 8.7 ni sozlang. `settings.gradle` va `build.gradle` tayyor.
5. SDK Manager orqali Android SDK Platform **35** ni o'rnating, JDK **17** ni tanlang.
6. **Build > Build Bundle(s) / APK(s) > Build APK(s)** ni bosing.
7. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## APK yaratish (GitHub Actions)

1. Bu papkadagi fayllarni GitHub'da yangi **private** repositoryga joylang (tokenlarni yuklamang!).
2. **Actions > Build Android APK > Run workflow** ni bosing yoki `main` branchga push qiling.
3. Yig'ish yakunlansa, ish natijasidagi `AIStudy-Companion-debug-APK` artifaktini yuklab oling.

**Eslatma:** bu muhitda Android SDK va Gradle yo'q, shu sababli APKni shu yerning o'zida yig'ish va Androidda sinash imkoni bo'lmadi. ZIP ichida to'liq Android Studio loyihasi bor.

## Token xavfsizligi

- Faqat o'zingizga tegishli va ruxsat berilgan akkauntdan foydalaning.
- Tokenni hech kimga, jumladan chatga yubormang.
- Kompyuter yoki telefonni boshqalarga berganda ilovani yopib tokenni xotiradan chiqarib yuboring.
- JWT payloadini o'qish autentifikatsiyani tasdiqlamaydi, yakuniy tekshiruvni server bajaradi.

## Texnik ma'lumotlar

- Java 17, Android Gradle Plugin 8.6.1, Gradle 8.7, compileSdk 35, targetSdk 34, minSdk 24.
- Java `HttpURLConnection`, `org.json`, Android SDK UI; tashqi runtime kutubxonalarsiz.
- Serverga faqat `https://api.aistudy.uz` ga GET so'rovi bor. Redirects o'chirilgan.
- Bu ilova AIStudy API ning rasmiy hujjati bilan emas, yuklangan desktop fayli tarkibidagi o'qish endpointi asosida yaratilgan. Kelajakda API o'zgarsa moslashtirish kerak bo'ladi.
