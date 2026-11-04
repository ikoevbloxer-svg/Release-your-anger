# Crack the Whip (NeoForge 1.21.1)

## Сборка
1. Установи JDK 21.
2. Если в папке нет `gradlew` — скопируй `gradlew`, `gradlew.bat` и папку `gradle/` из официального NeoForge MDK 1.21.1
   (github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle) или просто открой проект в IntelliJ IDEA.
3. `./gradlew build` — готовый мод появится в `build/libs/crackwhip-1.0.0.jar`.
4. Положи jar в `mods/` клиента (и сервера) NeoForge 1.21.1.
5. Тест из среды: `./gradlew runClient`.

## Предметы
- **Кнут** — ЛКМ: удар с досягаемостью 5 блоков; ПКМ: щелчок на 8 блоков. Урон 4 (2 сердца). Звук — твой crack_the_whip.
- **Цепной поводок** — ПКМ по мобу/игроку: посадить, ПКМ ещё раз: снять. Не рвётся.
