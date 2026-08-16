# Punchy × Epic Fight Compatibility

Mod exclusivamente cliente para Minecraft 1.20.1 Forge. Decide, por frame, si
la primera persona debe ser renderizada por Punchy o por Epic Fight. Conserva
la selección de objetivos de Epic Fight y solo corrige el caso de minería con
la mano vacía descrito más abajo. No cambia daño, perspectiva, skills ni
paquetes de red. Opcionalmente puede impedir salir del modo Epic Fight.

## Versiones objetivo

- Forge 47.4.22 / Java 17
- Punchy 2.7c
- Epic Fight 20.14.17
- Compatible de forma genérica con animaciones y `LivingMotion` de addons,
  incluyendo Epic Fight × ParCool 20.12.0.1.

## Instalación

1. Instala Punchy y Epic Fight en el cliente.
2. Elimina de la carpeta activa de mods cualquier versión de `epicpunchy`.
3. Copia `punchyepicfightcompat-1.0.16.jar` a `mods`.

El servidor no necesita este mod.

## Configuración

Toda la configuración vive en un único archivo:
`config/punchyepicfightcompat-client.toml`. Contiene las opciones de modo,
objetos y todas las animaciones descubiertas:

```toml
configVersion = 3

[mode]
forceEpicFightMode = true

[fixes]
emptyHandBlockBreaking = true

[rendering]
epicWeaponNamespaces = ["epicfight", "efn", "wom", "epicfightx", "invincible"]
forceEpicItems = []
forcePunchyItems = []

[animations."epicfight"]
"biped/combat/sword_auto_1" = true
"biped/living/idle" = false

[animations."epicparcool"]
"biped/hang_down_inertia" = true
"biped/hang_down" = true

[animations."wom"]
"biped/combat/torment_auto_1" = false
```

- Cada animación tiene exactamente una entrada específica.
- `true`: esa animación muestra el renderer de Epic Fight en primera persona.
- `false`: esa animación muestra Punchy en primera persona, incluso sosteniendo
  una espada o durante una acción de Epic Fight.
- Los encabezados `animations."namespace"` solamente ordenan las animaciones
  por mod. No activan ni desactivan addons y no cambian la prioridad.
- Esta configuración de animaciones no afecta la tercera persona.

Esta decisión por animación tiene prioridad absoluta sobre todas las reglas de
objetos. Si hay varias capas, manda primero la animación POV y después la capa
visible de mayor prioridad de Epic Fight. Como valor inicial, las familias de
acción principal, ataque, guardia, daño, esquiva y apuntado se marcan en
`true`. Los `LivingMotion` personalizados de addons (por ejemplo, permanecer
colgado, deslizarse por una pared o usar una tirolina con Epic Fight × ParCool)
también se marcan en `true`, aunque internamente sean animaciones estáticas. Los
movimientos ordinarios se marcan en `false`. Los valores ya
elegidos nunca se sobrescriben y los IDs nuevos se
incorporan automáticamente. El archivo se vuelve a leer aproximadamente cada
cinco segundos, por lo que puede editarse con el juego abierto. Un valor
malformado se ignora con un solo aviso en el log.

Al actualizar desde 1.0.10 o una versión anterior, el mod combina
`punchyepicfightcompat-client.toml` y
`punchyepicfightcompat-animations.toml` automáticamente. Conserva todos los
booleanos y mueve los archivos anteriores a copias con el nombre
`before-unified-config` antes de dejar activo el formato unificado.

La selección también se comprueba justo antes de que Punchy dibuje su modelo.
Esto cubre acciones instantáneas, como colocar un bloque, cuya capa de Epic
Fight puede comenzar después de la decisión general al inicio del fotograma,
y evita que ambos renderers queden visibles en ese mismo fotograma.

Cuando la animación activa todavía no tiene una entrada, se aplican las reglas
de objetos siguientes. Cada lista acepta IDs (`"minecraft:diamond_sword"`) y tags de objetos
(`"#forge:swords"`). Durante reposo, `forcePunchyItems` gana si una entrada
aparece en ambas listas. Una acción activa de Epic Fight siempre gana sobre
las dos listas. Sin overrides, un objeto con capability no vacía de Epic Fight
usa Epic; los demás usan Punchy. La mano principal decide, salvo cuando está
vacía, en cuyo caso decide la secundaria.

Las espadas (`SwordItem`, `#forge:swords` o `#forge:tools/swords`) usan Epic
Fight de forma continua, incluso si coinciden con `forcePunchyItems`. Fuera de
la minería, las armas con capability de combate solo se detectan automáticamente
si su namespace aparece en `epicWeaponNamespaces`. Los demás objetos usan Punchy.

Al atacar una entidad con cualquier objeto, Epic Fight toma el render durante
la acción y después devuelve el control a la regla normal. Mientras el juego ya
está rompiendo un bloque, las herramientas y la mano vacía pueden usar Punchy
como una decisión exclusivamente visual.

La versión 1.0.16 conserva el comportamiento de Epic Fight salvo por el fix
`emptyHandBlockBreaking`. Con la mano principal vacía, si la mira ya señala un
bloque dentro del alcance y no existe una entidad viviente atacable, visible y
dentro del alcance de ataque, el clic se deja a la minería vanilla. Si hay una
entidad cercana, Epic Fight conserva el clic y su animación de ataque. El fix no
elige objetivos ni se aplica a ningún objeto. Como Epic Fight puede dejar la
asignación vanilla del ataque en estado no presionado después de golpear el
aire, el fix comprueba el estado físico del botón y delega la rotura a los
métodos públicos de `MultiPlayerGameMode`. Minecraft conserva las validaciones,
el progreso y los paquetes vanilla. El estado físico solo se lee: el mod no
marca `keyAttack` como presionado, evitando que la minería se filtre a una
espada u otro objeto después de cambiar de slot. No se usan invokers de métodos privados.
La regla de minería se aplica igual en primera persona, tercera persona trasera
y tercera persona frontal; solo el arbitraje visual Punchy/Epic permanece
limitado a primera persona.

Con `forceEpicFightMode = true`, el jugador entra y permanece en battle mode y
la tecla de cambio no puede llevarlo a vanilla mode. Con `false`, Epic Fight
recupera su comportamiento de cambio de modo normal. Toda la selección de render
se desactiva cuando Epic Fight no está activo.

## Compilación

Los JAR de Epic Fight y Punchy son entradas externas de desarrollo y no se
redistribuyen. Por defecto, el proyecto busca ambos archivos en una carpeta
local `mods/`, que está excluida por Git. También puedes configurar
`local_mods_dir`, `epic_fight_jar` y `punchy_jar` en `gradle.properties` o
mediante propiedades `-P`, y ejecutar:

```text
gradlew build
```
