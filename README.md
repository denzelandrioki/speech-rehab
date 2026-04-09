# Speech Rehab (Речевая реабилитация)

Android-приложение для тренировки узнавания слов по картинкам: крупный интерфейс, минимум шагов, офлайн-кэш изображений и учёт статистики. Проект в экосистеме [TechLabHub](https://techlabhub.ru); идентификатор приложения: `ru.techlabhub.speechrehab`.

**Версия:** 0.1.0-mvp (minSdk 26, targetSdk 35).

## Возможности

- **Тренировка** — карточка «слово + изображение», отметки «верно» / «неверно», подсказка с текстом слова (настраивается).
- **Словарь** — включение и отключение слов по категориям; английский текст для поиска картинок, русские подписи в интерфейсе.
- **Настройки** — режим подбора карточек (случайный, сложные, новые, смешанный и др.), фильтр категорий, источники иллюстраций (ARASAAC, Pixabay, Pexels).
- **Статистика** — точность, динамика по дням/неделям, сложные и лёгкие слова, прогресс по категориям.

## Стек

| Категория | Технологии |
|-----------|------------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Hilt |
| Локальные данные | Room, DataStore Preferences |
| Сеть | Retrofit, OkHttp, Kotlin Serialization |
| Изображения | Coil |
| Прочее | Kotlin Coroutines, Timber |

## Требования к окружению

- **Android Studio** Ladybug или новее (рекомендуется последний стабильный канал).
- **JDK 17** (как в `compileOptions` / `kotlinOptions` модуля `app`).
- **Android SDK** с установленным API 35 для сборки.

## Клонирование и запуск

1. Клонируйте репозиторий и откройте **корень** проекта в Android Studio (тип проекта — Gradle).

2. В корне должен лежать **Gradle Wrapper** (`gradlew` / `gradlew.bat`). Сборка из терминала в корне:

   ```bash
   # Windows (PowerShell или cmd)
   .\gradlew.bat assembleDebug

   # Linux / macOS
   chmod +x gradlew
   ./gradlew assembleDebug
   ```

3. Файл **`local.properties`** создайте в корне (Android Studio обычно создаёт его сам) и при необходимости укажите `sdk.dir=...`. Файл в `.gitignore` — в git не коммитится.

## Ключи API (Pixabay / Pexels)

В **`local.properties`** можно добавить:

```properties
pixabay.api.key=ВАШ_КЛЮЧ_PIXABAY
pexels.api.key=ВАШ_КЛЮЧ_PEXELS
```

Ключи попадают в `BuildConfig` при сборке. **Без ключей** приложение собирается и запускается: для картинок остаются **ARASAAC** (без ключа) и уже **закэшированные** файлы; Pixabay/Pexels в этом случае не используются. **ARASAAC** — основной бесплатный источник пиктограмм для тренировки.

После изменения ключей выполните **Sync Project with Gradle Files**.

## Сборка

- **Debug:** `./gradlew assembleDebug` (или **Run** в Android Studio).
- **Unit-тесты:** `./gradlew test`
- **Релиз:** `./gradlew assembleRelease` (настройте подпись и храните keystore вне репозитория).

## Архитектура (кратко)

- Слой **`ui`** — Compose-экраны и ViewModel.
- **`domain`** — модели, сценарии (например, выбор следующей карточки), аналитика весов и статистики.
- **`data`** — Room, сетевые API, файловый кэш изображений, реализации репозиториев, DataStore.

Подробные пояснения по классам — в KDoc в исходниках.

## Данные и конфиденциальность

- База **`speech_rehab.db`** и кэш картинок хранятся в приватном каталоге приложения на устройстве.
- Ключи API не коммитьте: используйте только **`local.properties`** или безопасное хранилище секретов CI.

## Тесты

Юнит-тесты домена: `app/src/test/java` (например, `CardWeightEngineTest`, `StatisticsEngineTest`). Запуск: `./gradlew test`.

## CI

В репозитории есть workflow **GitHub Actions** (`.github/workflows/ci.yml`): сборка `assembleDebug` и `test` на push/PR в ветки `main` и `master`.

## Лицензия

[MIT](LICENSE)

## Автор и ссылки

- Профиль GitHub: [@denzelandrioki](https://github.com/denzelandrioki)  
- Сайт организации: [techlabhub.ru](https://techlabhub.ru)
