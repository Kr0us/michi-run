# Comprobaciones de la primera versión

Fecha: 19 de octubre de 2026.

## Compilación y análisis

Comando ejecutado en Windows con el JDK 21 de Android Studio:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console plain
```

Resultado: **BUILD SUCCESSFUL**. Las **13 pruebas unitarias** del motor pasan y Android Lint termina sin errores. Lint conserva cuatro advertencias: versiones posteriores de SDK y Gradle disponibles, target SDK anterior al más reciente y la recomendación de especificar las reglas de transferencia de datos de Android 12. El proyecto usa versiones fijadas y desactiva las copias de seguridad mediante `allowBackup=false`.

Las pruebas cubren: estado inicial, saltos, bloqueo de saltos en el aire, altura variable, colisiones, agacharse, pausa, equivalencia a 30 y 120 cuadros por segundo, recuperación de la partida, reintento, interrupciones largas del reloj, velocidad máxima y ciclo nocturno.

## Prueba en emulador

Se instaló el APK en el emulador **Pixel 10 Pro, imagen Android API 37.1**, con resolución de prueba de 720 × 1600. La imagen del emulador es una versión de desarrollo; el compile/target SDK del proyecto es 35.

Se verificaron estos recorridos:

- Abrir el menú, elegir un gatito gris y conservarlo al reiniciar la aplicación.
- Iniciar una partida y usar los botones de salto y agacharse.
- Pausar y comprobar que el puntaje queda detenido.
- Girar a horizontal y volver a vertical, conservando los 5 puntos y la partida en pausa.
- Continuar, chocar, mostrar 38 puntos y guardar el nuevo récord.
- Reintentar y volver al selector.
- Desactivar sonidos y cambiar a inglés desde los ajustes.
- Comprobar que el menú, los nombres de gatitos y los controles traducidos conservan la selección y el récord.

Las capturas de `screenshots/` proceden del APK instalado. No se ha realizado una prueba en teléfono físico ni una publicación en Google Play.
