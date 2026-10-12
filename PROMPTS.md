# Trazabilidad del arte generado

Todo el arte en `app/src/main/res/drawable-nodpi/` se generó con un modelo de
imagen (Google Flow) a partir de prompts descriptivos, sin referencias a obras,
artistas, marcas ni personajes. Después se procesó por lote con `ffmpeg`:

1. Recorte del fondo verde sólido → canal alfa (`colorkey`).
2. Recorte de márgenes transparentes (`alphaextract` + `bbox`).
3. Redimensionado al tamaño real de uso en pantalla.
4. Exportación WebP con calidad 84 (los fondos a calidad 82).

Total: **106 sprites, 2.13 MB**.

## Ancla de estilo usada en todos los prompts

```
cozy cottagecore herbal apothecary game art, hand-painted gouache with soft flat vector
shapes, warm muted palette of cream, oatmeal, sage green, terracotta, walnut brown and dusty
pink, soft rounded shapes, no harsh outlines, gentle warm diffused light, subtle paper grain,
clean composition, high detail, no text, no letters, no watermark
```

Negativo universal: `text, letters, words, watermark, signature, logo, UI, 3d render,
photorealistic, people faces`.

Cláusula para objetos recortables (evita que el modelo invente transparencia):

```
flat solid single-color [COLOR] background, uniform color, no pattern, no checkerboard,
no grid, no transparency effect, no gradient, no vignette, no drop shadow
```

## Mapa recurso → archivo original

Origen: `C:\Users\Demenx\Downloads\CozyHerbShop\`

| Recurso | Carpeta | Archivo generado |
|---|---|---|
| `bg_morning` / `sky_morning` | Escenarios | Cozy_garden_landscape_at_dawn |
| `bg_greenhouse` / `sky_greenhouse` | Escenarios | Cozy_greenhouse_interior |
| `bg_night` / `sky_night` | Escenarios | Fireflies_glowing_in_starry_meadow (la vista nocturna se recorta de él) |
| `bg_forest` / `sky_forest` | Escenarios | Bamboo_forest |
| `bg_autumn` / `sky_autumn` | Escenarios | Autumn_garden |
| `bg_loading` | UI y pantallas | Herbalist_shop_counter_with_jars |
| `fg_morning` | raíz | Meadow_grass_and_daisy_overlay (2ª generación) |
| `fg_forest` | raíz | Green_screen_cottagecore_game_ba (2ª generación) |
| `fg_greenhouse` | Escenarios | Hanging_ferns_and_trailing_pothos |
| `fg_night` | Escenarios | Leaves_and_grass_with_fireflies |
| `fg_autumn` | Escenarios | Maple_branches_and_fallen_leaves |
| `herb_chamomile` … `herb_thyme` | 8 Hierbas | los 8 espécimenes botánicos |
| `jar_empty`, `jar_layers`, `jar_sealed`, `jar_frozen`, `jar_hidden`, `jar_cork`, `wax_seal`, `fx_frost`, `fx_ripple` | Objetos partida | frascos, corcho, sello, escarcha, ondas |
| `teapot_copper`, `teapot_open` | Objetos partida | Copper_teapot |
| `currency_leaf`, `currency_bundle`, `token_undo`, `leaf_pip_full`, `leaf_pip_empty`, `icon_hint`, `icon_restart`, `icon_extra_jar` | Monedas,fichas,etc | recursos de interfaz |
| `fx_steam`, `fx_dust`, `fx_sparkle`, `fx_sparkle_green`, `fx_firefly`, `fx_leaf_a/b/c`, `fx_petal`, `fx_droplet`, `fx_rain`, `fx_snowflake`, `fx_glow`, `fx_confetti` | Particulas y efectos | partículas |
| `prop_*` (16) | Props de la tienda | mostrador, estantería, gato, mortero, miel, galletas, tazas, farol, alfombra, pizarra, hierbas colgadas, macetas, estufa, guirnalda, frascos |
| `prop_stone_lantern`, `prop_cushion`, `prop_bamboo_fountain`, `prop_koi`, `prop_moss_stone` | Modo zen | objetos de Yukata zen |
| `deco_shelf_oak`, `deco_fairy_garland`, `deco_stove_warm`, `deco_fern_pots` | Decoraciones en tienda | los 4 artículos de tienda |
| `asmr_cover_crystal`, `asmr_cover_rain`, `asmr_cover_fire` | Packs ASMR | los 3 packs de audio |
| `badge_maestro`, `badge_botanico`, `badge_ritual`, `badge_diseno` | Logros | los 4 logros |
| `ui_order_ticket`, `ui_letter`, `ui_scroll`, `ui_gift` | UI y pantallas | ticket, sobre, pergamino, regalo |
| `avatar_*` (13) | Clientes | retratos de los clientes |
| `ic_launcher_art` | Icono,etc | Copper_teapot_and_ceramic_cup (512x512 sin alfa, también en `Downloads/play_icon_512.png`) |
| `ic_launcher_bg` | Icono,etc | Gradient_background_with_leaf_silhouettes |
| `ic_launcher_fg` | Icono,etc | Copper_teapot_on_green_background (recorte del verde, encajado en la zona segura) |
| `ic_launcher_mono` | Icono,etc | derivado del alfa de la tetera (la silueta blacks tenía esquinas blancas) |
| `asmr_cover_stream` | F:\Contenido de imagenes\Packs ASMR | Water_pouring_into_stone_basin |
| `asmr_cover_forest` | F:\Contenido de imagenes\Packs ASMR | Cottagecore_game_sound_pack_cover |
| `asmr_cover_night` | F:\Contenido de imagenes\Packs ASMR | Firefly_glowing_near_night_window |
| `asmr_cover_crystal` | Packs ASMR | Cream_ceramic_bell (recorte central: el marco de destellos verdes no se puede transparentar) |
| `herb_mint` | raíz | Mint_sprig_on_magenta_background (2ª generación: relleno verde sólido) |

## Nota sobre derechos

Los prompts no mencionan obras, artistas, estudios, marcas ni personajes, y todas
las entidades representadas son objetos genéricos o PMCRE. La imagen resultante es
100 % sintética y se usa bajo los términos del generador empleado.
