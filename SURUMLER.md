# Sürüm düzeni

## Kural
- Biçim: `MAJOR.MINOR.PATCH` (ör. 1.1.6). Git etiketi ve uygulamadaki gösterim `v` önekiyle: `v1.1.6`.
- Küçük değişiklikte bile sürüm artar: her değişiklikte en az PATCH yükselir.
- Yeni özellik: MINOR artar, PATCH sıfırlanır. Büyük yeniden tasarım: MAJOR artar, diğerleri sıfırlanır.
- `VERSION_CODE` her sürümde 1 artar, asla düşmez.

## Seri adları
| Seri | Kod adı |
|------|---------|
| 1.x.x | Kalkan |
| 2.x.x | (henüz belirlenmedi) |

## Release adı
`Yeter! v<sürüm> · <seri adı> (APK)` — örnek: `Yeter! v1.1.6 · Kalkan (APK)`.
Etiket: `v<sürüm>`. Ad ve etiket iş akışında `version.properties`'ten otomatik üretilir.

## Sürüm artırırken dokunulacak dosyalar
1. `version.properties`: `VERSION_NAME`, `VERSION_CODE` (yeni seride `SERIES_NAME`).
2. `.github/release-notes.md`: başlıktaki sürüm ve değişiklik notları.
3. Bu tablo (yalnızca yeni seri açılırsa).

Uygulama içi sürüm gösterimi (⋮ menüsü) ve güncelleme kontrolü `version.properties`'ten türer; kodda elle sürüm yazılmaz.
