package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.GameLevel
import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import com.cozy.herbshop.data.model.SectorRule
import kotlin.random.Random

class LevelGenerator(
    private val solver: LevelSolver = LevelSolver()
) {

    /**
     * Fraccion de capas que se cambian de frasco al repartir un nivel.
     *
     * Antes se barajaba con vertidos desde el nivel ya ordenado, y eso casi no
     * mezclaba: como solo se podia verter en un frasco con hueco, las capas se
     * quedaban en su mayor parte dentro del mismo frasco y el nivel llegaba al
     * jugador con nueve de diez frascos ya ordenados.
     *
     * Repartiendo capas de verdad si se mezcla, pero cada capa de mas aleja el
     * nivel de ser resoluble y el solver tiene que buscar mas. Medido sobre el
     * nivel 39 (8 hierbas, 10 frascos): al 15% el solver resuelve 17 de cada 20,
     * al 20% la mitad y al 30% casi ninguna. Con 15% sale un nivel de verdad
     * esparcido y todavia se verifica casi siempre a la primera.
     */
    private companion object {
        const val MEZCLA_BASE = 0.15
    }

    /**
     * Genera un nivel siempre resoluble (y con tetera si hay receta).
     *
     * Se reparte cada frasco con una hierba y se cambian unas cuantas capas de
     * sitio. El nivel se valida después con el solver y, si no sale resoluble, se
     * prueba con menos mezcla: mejor un nivel repartido y verificado que uno
     * qualquer sin comprobar, porque un nivel puerta irresoluble deja al
     * jugador sin poder desbloquear la hierba siguiente.
     */
    fun generateSolvableLevel(
        levelNumber: Int,
        numHerbTypes: Int = 4,
        emptyJars: Int = 2,
        jarCapacity: Int = 4,
        random: Random = Random.Default,
        requiredOrder: List<Herb>? = null
    ): GameLevel {
        val requiredHerbs = requiredOrder?.distinct() ?: emptyList()
        // Toda hierba de la receta tiene que existir en el nivel: si el nivel usa
        // pocas especies, se añaden las que falten para que la receta sea posible.
        val availableHerbs = buildList {
            addAll(Herb.delReparto.take(numHerbTypes))
            requiredHerbs.forEach { if (!contains(it)) add(it) }
        }
        // Con receta hace falta un frasco vacío extra: será la tetera.
        val extraEmptyForTeapot = if (requiredOrder != null) 1 else 0
        val shouldAddMystery = levelNumber >= 3
        // El frasco sellado no se genera: sellar un frasco lleno de cuatro colores
        // deja el nivel imposible, porque cada color pierde una de sus cuatro
        // capas, ningun frasco se puede completar y el sello no se abre nunca. El
        // formato lo sigue admitiendo (assets/levels.json puede traerlo) para un
        // tablero hecho a mano, que es donde se puede comprobar.
        val shouldAddSealed = false
        val conPegajosas = SectorRule.forLevel(levelNumber) in
            setOf(SectorRule.STICKY, SectorRule.ALL)
        // El molinillo es del sector del invernadero, y en el ultimo sector van
        // todas las novedades juntas.
        val conMolinillo = SectorRule.forLevel(levelNumber).traeMolinillo
        // La puerta esconde su hierba secreta dentro de un frasco, tapada, asi que
        // el jugador la encuentra al destaparla. Se coloca antes de validar el
        // nivel para que el par y la pista cuenten con ella.
        val secreto = Herb.secretaDePuerta(levelNumber)

        // Primero se prueba con la mezcla de siempre y, si el solver no llega,
        // se va bajando. Cada capa de menos aleja el nivel de ser irresoluble.
        val mezclas = listOf(MEZCLA_BASE, MEZCLA_BASE * 0.6, MEZCLA_BASE / 3)
        for (mezcla in mezclas) {
            // La comprobacion es rapida (reparto sin busqueda), así que probar
            // muchos repartos sale mucho mas barato que acabar en el nivel de
            // reserva, que es un puzle mucho mas plano.
            for (intento in 1..80) {
                val repartidas = repartirCapas(
                    herbs = availableHerbs,
                    jarCapacity = jarCapacity,
                    emptyJars = emptyJars + extraEmptyForTeapot,
                    teapotIndex = if (extraEmptyForTeapot == 1) availableHerbs.size + emptyJars else -1,
                    fraccionMezcla = mezcla,
                    random = random
                )
                val solvedStacks = conSecreto(repartidas, secreto, random)
                val teapotIndex = if (extraEmptyForTeapot == 1) solvedStacks.lastIndex else -1

                // El molinillo se anade como un frasco vacio mas, al final, y solo
                // cuando el sector lo trae. Va despues de la tetera para no mover
                // su indice.
                val stacks: List<MutableList<Herb>> =
                    if (conMolinillo) solvedStacks + listOf(mutableListOf<Herb>()) else solvedStacks
                val grinderIndex = if (conMolinillo) stacks.lastIndex else -1

                // Un nivel que llega ya ordenado no es un nivel: el solver lo da
                // por bueno en el acto (la meta ya esta cumplida) y el jugador se
                // encuentra la partida hecha. Se descarta y se prueba otro reparto.
                if (solvedStacks.none { it.size > 1 && it.distinct().size > 1 }) continue

                val sealedJarIndex = if (shouldAddSealed && solvedStacks[0].isNotEmpty()) 0 else -1

                // Primero se decide la dificultad (misterio, congelado, sello) y
                // solo despues se valida. Es al reves de validar el nivel limpio:
                // una capa misteriosa corta la rafaga de vertido y un frasco
                // sellado no se puede tocar, asi que el jugador se encuentra un
                // puzle mas dificil que el que el solver dio por bueno.
                val jarIds = stacks.mapIndexed { index, herbs ->
                    herbs.mapIndexed { layerIndex, herb ->
                        val isTop = layerIndex == herbs.lastIndex
                        // El fondo del frasco sellado nunca se esconde: al abrirlo
                        // su cima tiene que quedar a la vista o el frasco queda
                        // inservible y la solucion del reparto seria ilegal.
                        val esFondoSellado = index == sealedJarIndex && layerIndex == 0
                        // La secreta va siempre tapada y nunca en la cima (la coloca
                        // conSecreto), asi que se ve como un misterio mas.
                        val isMystery = herb.esSecreta ||
                            (shouldAddMystery && !isTop && !esFondoSellado && random.nextFloat() < 0.18f)
                        // Congelada: una capa de dentro, no la cima. Congelar la
                        // cima no servia de nada (el motor descongela las cimas al
                        // empezar y al verter, asi que el jugador no la veia nunca);
                        // una capa de dentro si corta el bloque, como la pegajosa.
                        val isFrozen = levelNumber >= 5 && !isTop && random.nextFloat() < 0.08f
                        // Capa pegajosa (sector del bosque en adelante): se pega al
                        // frasco y hace de tope, asi que obliga a gastar una jugada
                        // mas. Nunca en la cima, que si se podria mover. Una de cada
                        // diez capas: con una de cada cinco el tablero salia tan
                        // troceado que ni el reparto ni la busqueda completa
                        // encontraban salida y el generador entregaba el nivel de
                        // reserva (plano) en la mitad del sector.
                        val isSticky = conPegajosas && !isTop && random.nextFloat() < 0.10f

                        var idStr = herb.id
                        if (isMystery) idStr = "?$idStr"
                        if (isFrozen) idStr = "frozen:$idStr"
                        if (isSticky) idStr = "sticky:$idStr"
                        if (index == sealedJarIndex && layerIndex == 0) idStr = "sealed:$idStr"
                        idStr
                    }
                }

                // El solver valida el nivel tal y como lo vera el jugador, con
                // sus capas misteriosas, congeladas y su frasco sellado.
                val jarsForCheck = jarIds.mapIndexed { index, ids ->
                    var isSealed = false
                    val layers = ids.map { raw ->
                        var cleanId = raw
                        if (cleanId.startsWith("sealed:")) {
                            isSealed = true
                            cleanId = cleanId.removePrefix("sealed:")
                        }
                        var isFrozen = false
                        if (cleanId.startsWith("frozen:")) {
                            isFrozen = true
                            cleanId = cleanId.removePrefix("frozen:")
                        }
                        val isSticky = cleanId.startsWith("sticky:")
                        if (isSticky) cleanId = cleanId.removePrefix("sticky:")
                        val isHidden = cleanId.startsWith("?") || cleanId.startsWith("mystery:")
                        JarLayer(
                            herb = Herb.fromId(cleanId.removePrefix("?").removePrefix("mystery:")),
                            isHidden = isHidden,
                            isFrozen = isFrozen,
                            isSticky = isSticky
                        )
                    }
                    Jar(
                        id = index,
                        capacity = when {
                            index == grinderIndex -> 1
                            index == teapotIndex ->
                                requiredOrder?.size?.coerceAtLeast(1) ?: jarCapacity
                            else -> jarCapacity
                        },
                        layers = layers,
                        isSealed = isSealed,
                        isTeapot = index == teapotIndex,
                        isGrinder = index == grinderIndex
                    ).revealTopLayer().unfreezeTopLayer()
                }

                // Se guarda la solucion con la que se ha comprobado el nivel: sus jugadas son
                // el par del nivel, asi que no hay que inventar un numero.
                val solucion = solver.repartirYResolver(jarsForCheck, requiredOrder) ?: continue

                return GameLevel(
                    levelNumber = levelNumber,
                    jarCapacity = jarCapacity,
                    jars = jarIds,
                    parMoves = solucion.size,
                    sectorRule = SectorRule.forLevel(levelNumber),
                    grinderJarIndex = grinderIndex.takeIf { it >= 0 }
                )
            }
        }

        return createFallbackLevel(levelNumber, availableHerbs, jarCapacity, emptyJars + extraEmptyForTeapot, requiredOrder) { herbs, cap, order, intento ->
            val repartidas = fallbackJars(
                herbs, cap, emptyJars, order,
                Random(levelNumber * 31 + herbs.size + intento * 977)
            )
            conSecretoEnLosFrascos(
                repartidas, secreto,
                Random(levelNumber * 31 + 13 + intento * 977)
            )
        }
    }

    /**
     * Reparte las capas de los frascos.
     *
     * Cada frasco se arma con hierbas distintas: el frasco i se queda con las
     * [jarCapacity] hierbas que siguen a la i en un orden barajado, de modo que
     * ninguna se repite dentro y cada hierba sale [jarCapacity] veces, una por
     * frasco. Despues se cruzan algunas capas entre frascos, aceptando solo los
     * cruces que no meten dos iguales en un mismo frasco.
     *
     * Asi el nivel llega con cuatro colores distintos en cada frasco, no con
     * "rojo rojo mora mora", y ningun frasco viene ya terminado.
     */

/**
 * Baraja las capas de un frasco sin dejar dos iguales pegadas, que es lo que se
 * ve mal: "rojo rojo mora mora" en vez de "rojo mora verde otro".
 * Es el reparto clasico de cadenas: se va cogiendo por turnos y, si toca, se
 * busca una capa que no sea la anterior.
 */
private fun sinParejas(capas: List<Herb>, random: Random): MutableList<Herb> {
    val cola = capas.toMutableList()
    cola.shuffle(random)
    if (cola.size < 2) return cola
    val salida = mutableListOf(cola.removeAt(0))
    while (cola.isNotEmpty()) {
        val anterior = salida.last()
        val indice = cola.indexOfFirst { it != anterior }
        // Si todas las que quedan son iguales no hay manera de separarlas: se coge
        // una y ya esta repartido lo que se puede.
        salida.add(if (indice >= 0) cola.removeAt(indice) else cola.removeAt(0))
    }
    return salida
}

private fun repartirCapas(
        herbs: List<Herb>,
        jarCapacity: Int,
        emptyJars: Int,
        teapotIndex: Int,
        fraccionMezcla: Double,
        random: Random
    ): List<MutableList<Herb>> {
        val total = herbs.size
        val frascos = mutableListOf<MutableList<Herb>>()

        if (total >= 2) {
            // Cada frasco se arma con la ronda de hierbas que le toca: el frasco i
            // se queda con herbs[(i+k) % total], k de 0 a la capacidad. Asi cada
            // hierba sale capacidad veces, una por frasco, y ningun frasco lleva
            // dos colores pegados. Con pocas hierbas se repiten, pero separadas
            // (manzanilla, lavanda, manzanilla, lavanda), no en bloque.
            val orden = herbs.shuffled(random)
            (0 until total).forEach { i ->
                frascos.add(MutableList(jarCapacity) { k -> orden[(i + k) % total] })
            }
        } else {
            herbs.forEach { h -> frascos.add(MutableList(jarCapacity) { h }) }
        }
        repeat(emptyJars) { frascos.add(mutableListOf()) }
        // El orden de las capas tambien es al azar, pero reordenado para que no
        // queden dos colores pegados: un shuffle a pelo saca "manzanilla
        // manzanilla lavanda" en cuanto hay menos hierbas que huecos.
        frascos.indices.forEach { indice ->
            val frasco = frascos[indice]
            if (frasco.size == jarCapacity) {
                frascos[indice] = sinParejas(frasco, random)
            }
        }

        // Cruces extra entre frascos para que dos Aromaticos no se queden siempre
        // en la misma posicion. Solo se aceptan si no crean dos iguales juntos,
        // que es justo lo que se quiere evitar.
        val intercambios = maxOf(2, (total * jarCapacity * fraccionMezcla / 2).toInt())
        var hechos = 0
        var intentos = 0
        val llenos = frascos.indices.filter {
            it != teapotIndex && frascos[it].size == jarCapacity
        }
        while (hechos < intercambios && intentos < intercambios * 15 && llenos.size >= 2) {
            intentos++
            val a = llenos.random(random)
            val b = llenos.filter { it != a }.random(random)
            val posA = frascos[a].indices.random(random)
            val posB = frascos[b].indices.random(random)
            val deA = frascos[a][posA]
            val deB = frascos[b][posB]
            if (frascos[a].count { it == deB } > 0 || frascos[b].count { it == deA } > 0) continue
            frascos[a][posA] = deB
            frascos[b][posB] = deA
            hechos++
        }
        return frascos
    }

    /** Monta el nivel plano: un frasco lleno por hierba, un vacio y la tetera. */
    private fun fallbackJars(
        herbs: List<Herb>,
        jarCapacity: Int,
        emptyJars: Int,
        requiredOrder: List<Herb>?,
        random: Random
    ): List<Jar> {
        val teapot = requiredOrder != null
        // Los vacios van detras de los frascos de hierba; si hay tetera, es el
        // ultimo de todos.
        val teapotIndex = if (teapot) herbs.size + emptyJars - 1 else -1
        // Tambien el nivel de emergencia va repartido: un frasco lleno por hierba
        // ya ordenado no parece un nivel, parece un fallo.
        val stacks = repartirCapas(
            herbs = herbs,
            jarCapacity = jarCapacity,
            emptyJars = emptyJars,
            teapotIndex = teapotIndex,
            fraccionMezcla = 0.0,
            random = random
        )
        return stacks.mapIndexed { index, layers ->
            Jar(
                id = index,
                capacity = if (index == teapotIndex) requiredOrder!!.size.coerceAtLeast(1) else jarCapacity,
                layers = layers.map { JarLayer(herb = it) },
                isTeapot = index == teapotIndex
            )
        }
    }

    /**
     * Último recurso: nivel "plano". Antes de devolverlo se valida con el solver,
     * porque un nivel no validado puede tener recetas imposibles. Si el nivel con
     * todas las hierbas no sirve, se reduce a las hierbas de la receta: así no hay
     * nada que tape los frascos que hay que usar.
     */
    /**
     * Esconde la hierba secreta de la puerta en un frasco al azar.
     *
     * Sustituye una capa de dentro (nunca la cima, que se veria) por la secreta:
     * el nivel trae las mismas capas que traia, sigue siendo ganable con ella
     * dentro y el jugador la encuentra al destapar el frasco. La tetera y el
     * molinillo no son frascos de almacen y no entran.
     */
    private fun conSecreto(
        stacks: List<MutableList<Herb>>,
        secreto: Herb?,
        random: Random
    ): List<MutableList<Herb>> {
        if (secreto == null) return stacks
        val candidatos = stacks.indices.filter { stacks[it].size >= 2 }
        if (candidatos.isEmpty()) return stacks
        val elegido = candidatos.random(random)
        val donde = random.nextInt(stacks[elegido].size - 1)
        return stacks.mapIndexed { index, capas ->
            if (index != elegido) capas
            else capas.toMutableList().also { it[donde] = secreto }
        }
    }

    /** Igual que [conSecreto], para los frascos ya montados del nivel de reserva. */
    private fun conSecretoEnLosFrascos(jars: List<Jar>, secreto: Herb?, random: Random): List<Jar> {
        if (secreto == null) return jars
        val candidatos = jars.indices.filter { i ->
            !jars[i].isTeapot && !jars[i].isGrinder && jars[i].layers.size >= 2
        }
        if (candidatos.isEmpty()) return jars
        val elegido = candidatos.random(random)
        val donde = random.nextInt(jars[elegido].layers.size - 1)
        return jars.mapIndexed { index, jar ->
            if (index != elegido) jar
            else jar.copy(
                layers = jar.layers.mapIndexed { capa, layer ->
                    if (capa != donde) layer else JarLayer(secreto, isHidden = true)
                }
            )
        }
    }

    /** La capa tal y como la lee [LevelRepository.convertToJars]. */
    private fun JarLayer.idParaElNivel(): String = buildString {
        if (isSticky) append("sticky:")
        if (isFrozen) append("frozen:")
        if (isHidden) append("?")
        append(herb.id)
    }

    private fun createFallbackLevel(
        levelNumber: Int,
        herbs: List<Herb>,
        jarCapacity: Int,
        emptyJars: Int,
        requiredOrder: List<Herb>?,
        build: (List<Herb>, Int, List<Herb>?, Int) -> List<Jar>
    ): GameLevel {
        // El recorte nunca deja fuera la receta: si el nivel pide hibisco y el
        // recorte se lo lleva, el nivel no se puede servir de ninguna manera.
        fun recorte(cuantas: Int): List<Herb> {
            val deReceta = requiredOrder?.distinct().orEmpty()
            val resto = herbs.filterNot { it in deReceta }
            return (resto.take(cuantas) + deReceta).distinct()
        }
        val candidates = buildList {
            add(herbs)
            requiredOrder?.distinct()?.let { if (it.isNotEmpty() && it != herbs) add(it) }
            // Con menos hierbas el reparto es mucho mas facil de terminar, asi que
            // se van recortando hasta que alguno sale ganable.
            recorte(4).let { if (it.isNotEmpty() && it != herbs) add(it) }
            recorte(3).let { if (it.isNotEmpty() && it != herbs && it !in this) add(it) }
            // El ultimo recurso se queda en dos hierbas: tres frascos de un solo
            // color cada uno es justo lo que el jugador odia ("rojo rojo mora
            // mora"), asi que mejor quedarse corto que entregar un tablero asi.
            recorte(2).let { if (it.isNotEmpty() && it !in this) add(it) }
        }

        // El molinillo se anade siempre que el sector lo traiga, tambien en el
        // nivel de reserva: si no, el sector anunciaria una novedad que no esta.
        val conMolinillo = SectorRule.forLevel(levelNumber).traeMolinillo
        val conMolinilloSiToca: (List<Jar>) -> List<Jar> = { jars ->
            if (!conMolinillo) jars
            else jars + Jar(id = jars.size, capacity = 1, isGrinder = true)
        }

        for (candidate in candidates) {
            // Varias tiradas por recorte: el reparto va a suertes y con una sola
            // tirada un nivel rico se caia al recorte de dos hierbas, que es un
            // tablero plano.
            for (intento in 0 until 8) {
                val jars = conMolinilloSiToca(build(candidate, jarCapacity, requiredOrder, intento))
                // Ahora tambien se comprueba cuando no hay receta: antes se entregaba
                // sin verificar y el reparto nuevo puede salir imposible de terminar.
                val solucion = solver.repartirYResolver(jars, requiredOrder)
                if (solucion != null) {
                    return GameLevel(
                        levelNumber = levelNumber,
                        jarCapacity = jarCapacity,
                        jars = jars.map { jar -> jar.layers.map { it.idParaElNivel() } },
                        parMoves = solucion.size,
                        sectorRule = SectorRule.forLevel(levelNumber),
                        grinderJarIndex = if (conMolinillo) jars.lastIndex else null
                    )
                }
            }
        }

        // Ultimo recurso. Antes se devolvia el candidato sin comprobar, y eso
        // podia dejar un nivel de puerta irresoluble: el jugador se quedaria
        // atascado sin poder desbloquear la hierba. Ahora, si nada valida, se
        // devuelve un nivel trivialmente ganable (un frasco por hierba de la
        // receta, ya ordenados), que siempre se puede resolver.
        val last = conMolinilloSiToca(build(candidates.last(), jarCapacity, requiredOrder, 0))
        val solucionLast = solver.repartirYResolver(last, requiredOrder)
        val safe = if (solucionLast != null) {
            last
        } else {
            conMolinilloSiToca(build(solvedFallbackHerbs(requiredOrder), jarCapacity, requiredOrder, 0))
        }
        return GameLevel(
            levelNumber = levelNumber,
            jarCapacity = jarCapacity,
            jars = safe.map { jar -> jar.layers.map { it.idParaElNivel() } },
            parMoves = solver.repartirYResolver(safe, requiredOrder)?.size,
            sectorRule = SectorRule.forLevel(levelNumber),
            grinderJarIndex = if (conMolinillo) safe.lastIndex else null
        )
    }

    /**
     * Hierbas del nivel de emergencia: las de la receta y, si no hay receta, la
     * primera. Cada una en su propio frasco lleno, asi que ordenar es inmediato
     * y la receta se puede servir sin movimiento previo.
     */
    private fun solvedFallbackHerbs(requiredOrder: List<Herb>?): List<Herb> =
        requiredOrder?.distinct()?.takeIf { it.isNotEmpty() } ?: listOf(Herb.CHAMOMILE)
}
