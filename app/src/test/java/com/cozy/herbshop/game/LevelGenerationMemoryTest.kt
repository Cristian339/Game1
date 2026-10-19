package com.cozy.herbshop.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los niveles altos se comian la memoria y la app caia.
 *
 * El generador baraia el nivel con muchos vertidos y luego lo verificaba con una
 * busqueda que guarda cada estado en memoria. Con 8 hierbas y 10 frascos eso son
 * cientos de miles de estados, muy por encima del heap de un movil: entrar en el
 * nivel 39 terminaba en OutOfMemoryError y la partida se caia.
 *
 * El test corre con el mismo techo de memoria que el telefono (192 MB, lo fija
 * build.gradle.kts), asi que si vuelve a pasarse, falla aqui antes de llegar al
 * jugador.
 */
class LevelGenerationMemoryTest {

    @Test
    fun losNivelesAltosSeGeneranDentroDelHeap() {
        val repository = LevelRepository(null)
        val fallos = mutableListOf<String>()
        // Los niveles con 8 hierbas (a partir del 27) son los que revientaban, y
        // las puertas 40 y 50 son las mas grandes porque llevan tetera.
        listOf(27, 30, 33, 36, 39, 40, 45, 50).forEach { numero ->
            val inicio = System.currentTimeMillis()
            val nivel = runCatching { repository.getLevel(numero) }
                .onFailure { fallos.add("nivel $numero: ${it::class.simpleName} ${it.message?.take(60)}") }
                .getOrNull() ?: return@forEach
            val pasado = System.currentTimeMillis() - inicio
            val jars = repository.convertToJars(nivel)
            assertTrue("el nivel $numero salio vacio", jars.isNotEmpty())
            assertTrue(
                "el nivel $numero tardo ${pasado}ms en generarse",
                pasado < 8_000
            )
        }
        assertTrue("niveles que no se pudieron generar: $fallos", fallos.isEmpty())
    }

    @Test
    fun casiNingunFrascoTieneDosColoresJuntos() {
        // Regla del jugador: nada de "rojo rojo mora mora". Se quiere "rojo, mora,
        // verde, otro", y como mucho se admite un frasco o dos con un par de
        // colores pegados. Se mide por parejas contiguas, no por repetidas: un
        // frasco con manzanilla, lavanda, manzanilla y lavanda va alternado y
        // cumple, aunque repita color.
        val repository = LevelRepository(null)
        val demasiados = mutableListOf<String>()
        (1..50).forEach { numero ->
            repeat(3) { intento ->
                val jars = repository.convertToJars(repository.getLevel(numero))
                    .filterNot { it.isTeapot }
                val conPareja = jars.count { jar ->
                    // zipWithNext sobre un frasco de una sola capa se compara
                    // consigo mismo, asi que hace falta al menos dos.
                    jar.layers.size >= 2 &&
                        jar.layers.zipWithNext().any { (arriba, abajo) -> arriba.herb == abajo.herb }
                }
                if (conPareja > 2) {
                    demasiados.add("$numero/$intento con $conPareja frascos emparejados")
                }
            }
        }
        assertTrue(
            "niveles con mas de dos frascos de colores juntos: ${demasiados.take(8)}",
            demasiados.isEmpty()
        )
    }

    @Test
    fun ningunNivelEmpiezaConFrascosYaTerminados() {
        // Un frasco lleno y de una sola hierba ya esta hecho: el jugador se
        // encuentra niveles con dos o tres de esos sin haber movido nada. Se
        // comprueba en los cincuenta niveles, no solo en los altos, porque salia
        // en cualquier sector.
        val repository = LevelRepository(null)
        val listos = mutableListOf<String>()
        (1..50).forEach { numero ->
            repeat(3) { intento ->
                val jars = repository.convertToJars(repository.getLevel(numero))
                    .filterNot { it.isTeapot || it.isGrinder }
                jars.forEachIndexed { indice, jar ->
                    if (jar.layers.size == jar.capacity && jar.layers.map { it.herb }.distinct().size == 1) {
                        listos.add("$numero/$intento frasco ${indice + 1}")
                    }
                }
            }
        }
        assertTrue(
            "frascos que ya vienen llenos de una sola hierba: ${listos.take(8)}",
            listos.isEmpty()
        )
    }

    @Test
    fun losNivelesNoVienenCasiOrdenados() {
        // Comprobacion mas floja, por si acaso: tampoco vale que nueve de diez
        // frascos salgan puros.
        val repository = LevelRepository(null)
        val planos = mutableListOf<String>()
        listOf(27, 30, 33, 36, 39, 40, 45, 50).forEach { numero ->
            repeat(6) {
                val jars = repository.convertToJars(repository.getLevel(numero))
                    .filterNot { it.isTeapot || it.isGrinder }
                val ordenados = jars.count { jar ->
                    jar.layers.isNotEmpty() && jar.layers.map { it.herb }.distinct().size == 1
                }
                if (ordenados >= jars.size - 1 && jars.size > 2) planos.add("$numero ($ordenados/${jars.size})")
            }
        }
        assertTrue(
            "niveles que salieron ya ordenados, sin nada que resolver: ${planos.take(6)}",
            planos.isEmpty()
        )
    }
}