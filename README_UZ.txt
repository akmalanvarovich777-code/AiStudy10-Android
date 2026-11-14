AISTUDY10 ANDROID 1.0 — ANDROID STUDIO LOYIHASI
Muallif: Akmal Muxamedjanov

DIQQAT: Bu ZIP ichida tayyor .apk YO'Q. Android Studio uchun source loyiha bor.

APKga aylantirish (Windows):
1. Android Studio ni https://developer.android.com/studio dan o'rnating.
2. Android Studio -> SDK Manager bo'limidan Android SDK Platform API 35 o'rnating.
3. ZIPni to'liq chiqarib, ushbu papkadagi APK_YASASH_WINDOWS.bat faylini bosing.
   Internet talab qilinadi; Gradle va Android build komponentlari yuklanadi.
4. Jarayon muvaffaqiyatli tugasa, AiStudy10_Android_debug.apk hosil bo'ladi.
5. Faylni telefonga ko'chiring va APK o'rnatishga ruxsat bering.

Yoki Android Studio -> Open -> AiStudy10_Android_Studio papkasini tanlang.
Gradle sync tugagach Build -> Build APK(s) ni bosing.
Tayyor APK: app/build/outputs/apk/debug/app-debug.apk

YOKI GitHub Actions: kodni GitHub repository'ga yuklab,
Actions -> AiStudy10 Android APK -> Run workflow ni bosing.
Actions tugagach AiStudy10_Android_APK artifact ichida debug APK bo'ladi.

Dastur xususiyatlari:
- Android 9 (API 28) yoki undan yangi Android;
- 10 ta mustaqil WebView profili (Android process va alohida cookie papkalari);
- sayt ichida qo'lda login, parollar ilovaning Java kodida saqlanmaydi;
- 1x, 1.5x, 2x, 3x video tezligi (sayt ruxsat bersa);
- oxirgi video va vaqtni WebView localStorage orqali saqlash va tiklashga urinish;
- video tugaganda navbatdagi ko'rilmagan darsni ochishga urinish;
- test yordamchisi (faqat ma'lum 38 ta bazadagi savollarga moslik qidiradi,
  avtomatik topshirmaydi; noto'g'ri javob ehtimoli bor);
- ilova ichida foydalanish yo'riqnomasi va muallif ismi.

CHEKLOVLAR:
- Bu Windows Electron dasturining aynan bir xil APK porti emas;
  mahalliy Android WebView asosida qayta yozilgan DASTLABKI versiya.
- Android fon jarayonlarini to'xtatishi mumkin. 10 ta videoni ayni paytda
  uzluksiz fonda ko'rish KAFOLATLANMAYDI.
- Har safar faqat bir profil ekranda ochiladi. Profillar o'zaro cookie almashmaydi.
- Saytning iframe pleerlari 3x/autoplay kabi funksiyalarni cheklashi mumkin.
- «Ko'rildi» belgisi faqat sayt serveri tomonidan tasdiqlanadi, ilova uni soxtalashtirmaydi.
- Test avtomatik yakunlanmaydi. Testni yuborish foydalanuvchi nazoratida.
- OMP saytidagi haqiqiy login/test sahifalari bilan sinovdan o'tkazilmagan.
- Dastur mustaqil yordamchi, omp.aistudy.uz rasmiy Android ilovasi emas.
- Faqat sizga kirishga ruxsat etilgan akkauntlarda foydalaning.

Agar Build xatosi chiqsa, build oynasidagi birinchi "error" qatorlarini yuboring.
