# Karınca — Yayın Öncesi Eksikler

Kısa durum raporu: neyin eksik olduğu ve neden gerektiği.

## 1. Google ile giriş
Kod hazır (`GoogleSignInHelper.kt`), ama `BuildConfig.GOOGLE_WEB_CLIENT_ID` boş —
Google Cloud Console'da bir Web + Android OAuth client oluşturulup Web Client ID'nin
hem `local.properties`'e (`google.webClientId=...`) hem de Supabase Auth →
Providers → Google ayarına girilmesi gerekiyor. Debug keystore SHA-1:
`49:45:FA:C2:CC:E8:0C:15:E9:03:14:9E:5B:67:19:91:9C:72:49:0F`

## 2. Abonelik sistemi (RevenueCat / Play Billing)
Backend tarafı hazır: `entitlements` tablosu + haftalık rapor cron'unun Pro
olmayanları atlaması. Google Play Console kimlik doğrulaması tamamlanmalı,
RevenueCat hesabı açılmalı; ardından client'a Purchases SDK eklenip paywall
ekranı (tasarım/metin kullanıcıdan) yapılmalı.

## 3. Şifre/e-posta değişikliğinde doğrulama
Kişisel Bilgiler'den bu bilgiler değiştirilirken (varsa) mevcut şifre onayı ya da
e-posta değişikliğinde onay linki gibi ekstra bir doğrulama adımı yok — hesap ele
geçirme riskine karşı eklenmesi gerekiyor.

## 4. Bildirim sistemi
Push bildirim altyapısı (örn. FCM) hiç kurulmamış. Haftalık rapor hazır olduğunda,
bütçe limitine yaklaşıldığında/aşıldığında gibi durumlarda kullanıcıya haber
verilemiyor.

## 5. "Şifremi Unuttum" çalışmıyor
`SignInScreen.kt`'de bu sadece statik bir metin, `onClick`'i yok. Gerçek bir parola
sıfırlama akışı (Supabase'in `resetPasswordForEmail` çağrısı + e-posta linki +
yeni şifre ekranı) eklenmesi gerekiyor.

## 6. Hesap silme yok
Şema migration'ında bilerek not edilmiş: profil satırı sadece hesap silme ile
kaldırılabiliyor ama bu henüz client'ta bir akış değil. KVKK'nın "unutulma hakkı"
gereksinimi düşünülürse yayın öncesi gerekebilir — hem "Hesabımı Sil" ekranı hem
sunucu tarafında gerçek silme akışı gerekiyor.

## 7. Kilitlenme/crash raporlama yok
Firebase Crashlytics veya benzeri hiç entegre edilmemiş. Yayında bir kullanıcı
çökme yaşarsa bundan haberimiz olmuyor.

## 8. Otomatik test yok
Projede tek bir unit/UI testi bile yok. Özellikle Kumbara, aylık gelir ve hedef
ilerleme gibi para hesaplayan mantık büyüdükçe, bir değişikliğin başka bir yeri
sessizce bozup bozmadığını anlamanın tek yolu şu an elle test etmek.

## 9. Release imzalama / Play Store yayın hazırlığı doğrulanmadı
`keystore.properties`'in gerçekten dolu olup olmadığı, ProGuard/R8 kuralları,
store listing varlıkları (ikon, ekran görüntüleri, açıklama metni) henüz kontrol
edilmedi.
