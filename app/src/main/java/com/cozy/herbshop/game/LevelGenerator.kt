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
        const val INTENTOS_POR_PASADA = 80
    }

    /**
     * Cuanto adorno lleva un intento de reparto: la mezcla entre frascos y si
     * se anaden capas misteriosas extra. Las mecanicas del sector (hielo,
     * molinillo, miel) ya no son opcionales: van siempre en su tramo y el solver
     * valida el tablero con ellas puestas. Solo el misterio extra se puede
     * quitar en las pasadas mas conservadoras.
     */
    private data class Adorno(
        val fraccionMezcla: Double,
        val misterioExtra: Boolean
    )

    private data class Marcas(
        val misterio: Set<Pair<Int, Int>>,
        val helado: Int?,
        val pegajosa: Set<Pair<Int, Int>>
    )

    private data class TableroPreparado(
        val stacks: List<MutableList<Herb>>,
        val teapotIndex: Int,
        val grinderIndex: Int,
        val marcas: Marcas
    )

    /**
     * Genera un nivel siempre resoluble (y con tetera si hay receta).
     *
     * Se reparte cada frasco con una hierba y se cambian unas cuantas capas de
     * sitio. Antes de validar se anaden las mecanicas del sector, y ademas se
     * crea el desequilibrio que hace obligatorio el molinillo. El nivel se
     * valida despues con el solver y, si no sale resoluble, se prueba con menos
     * mezcla: mejor un nivel repartido y verificado que uno cualquiera sin
     * comprobar, porque un nivel puerta irresoluble deja al jugador sin poder
     * desbloquear la hierba siguiente.
     */
    fun generateSolvableLevel(
        levelNumber: Int,
        numHerbTypes: Int = 4,
        emptyJars: Int = 1,
        jarCapacity: Int = 4,
        random: Random = Random.Default,
        requiredOrder: List<Herb>? = null
    ): GameLevel {
        val sector = SectorRule.forLevel(levelNumber)
        val requiredHerbs = requiredOrder?.distinct() ?: emptyList()
        // Toda hierba de la receta tiene que existir en el nivel: si el nivel usa
        // pocas especies, se anaden las que falten para que la receta sea posible.
        val availableHerbs = buildList {
            addAll(Herb.delReparto.take(numHerbTypes))
            requiredHerbs.forEach { if (!contains(it)) add(it) }
        }
        // Con receta hace falta un frasco vacio extra: sera la tetera.
        val extraEmptyForTeapot = if (requiredOrder != null) 1 else 0
        val conMolinillo = sector.traeMolinillo
        // La puerta esconde su hierba secreta dentro de un frasco, tapada, asi que
        // el jugador la encuentra al destaparla. Se coloca antes de validar el
        // nivel para que el par y la pista cuenten con ella.
        val secreto = Herb.secretaDePuerta(levelNumber)

        // Primero se prueba con la mezcla de siempre y, si el solver no llega,
        // se va bajando. Las mecanicas del sector se quedan en todas las
        // pasadas: si se quitaran, la ficha y el tutorial mentirian.
        val pasadas = listOf(
            Adorno(MEZCLA_BASE, misterioExtra = true),
            Adorno(MEZCLA_BASE * 0.6, misterioExtra = true),
            Adorno(MEZCLA_BASE / 3, misterioExtra = true),
            Adorno(MEZCLA_BASE, misterioExtra = false),
            Adorno(0.0, misterioExtra = false)
        )
        for (adorno in pasadas) {
            // La comprobacion es rapida (reparto sin busqueda), asi que probar
            // muchos repartos sale mucho mas barato que acabar en el nivel de
            // reserva, que es un puzle mucho mas plano.
            for (intento in 1..INTENTOS_POR_PASADA) {
                val repartidas = repartirCapas(
                    herbs = availableHerbs,
                    jarCapacity = jarCapacity,
                    emptyJars = emptyJars + extraEmptyForTeapot,
                    teapotIndex = if (extraEmptyForTeapot == 1) availableHerbs.size + emptyJars else -1,
                    fraccionMezcla = adorno.fraccionMezcla,
                    random = random
                )
                val teapotIndex = if (extraEmptyForTeapot == 1) repartidas.lastIndex else -1

                val preparado = prepararTablero(
                    base = repartidas,
                    teapotIndex = teapotIndex,
                    conMolinillo = conMolinillo,
                    requiredOrder = requiredOrder,
                    availableHerbs = availableHerbs,
                    secreto = secreto,
                    levelNumber = levelNumber,
                    sector = sector,
                    adorno = adorno,
                    random = random
                ) ?: continue

                // Un nivel que llega ya ordenado no es un nivel: el solver lo da
                // por bueno en el acto (la meta ya esta cumplida) y el jugador se
                // encuentra la partida hecha. Se descarta y se prueba otro reparto.
                val normales = preparado.stacks.indices.filter {
                    it != preparado.teapotIndex && it != preparado.grinderIndex
                }
                if (normales.none { i ->
                        preparado.stacks[i].size > 1 &&
                            preparado.stacks[i].distinct().size > 1
                    }
                ) continue

                val jarsForCheck = aJars(preparado, jarCapacity, requiredOrder)
                // El solver valida el nivel tal y como lo vera el jugador. La
                // solucion es tambien el par de jugadas.
                val solucion = solver.repartirYResolver(jarsForCheck, requiredOrder) ?: continue
                return nivelDesde(levelNumber, jarsForCheck, preparado, solucion)
            }
        }

        return createFallbackLevel(
            levelNumber = levelNumber,
            herbs = availableHerbs,
            jarCapacity = jarCapacity,
            emptyJars = emptyJars,
            requiredOrder = requiredOrder,
            secreto = secreto
        )
    }

    /**
     * Reparte, esconde la secreta, anade el molinillo y sus capas, y decide las
     * marcas del sector. Devuelve null si ese reparto no permite cumplir las
     * reglas (por ejemplo, no queda ningun frasco con hueco interior donde
     * poner la miel).
     */
    private fun prepararTablero(
        base: List<MutableList<Herb>>,
        teapotIndex: Int,
        conMolinillo: Boolean,
        requiredOrder: List<Herb>?,
        availableHerbs: List<Herb>,
        secreto: Herb?,
        levelNumber: Int,
        sector: SectorRule,
        adorno: Adorno,
        random: Random
    ): TableroPreparado? {
        val stacks = conSecreto(base, secreto, random)
            .map { it.toMutableList() }
            .toMutableList()
        val grinderIndex = if (conMolinillo) {
            stacks.add(mutableListOf())
            stacks.lastIndex
        } else {
            -1
        }

        // El molinillo tiene que hacer algo de verdad. En los niveles sin receta
        // se crea un desequilibrio (5 de A y 3 de B, con A.molida == B) para que
        // la unica forma de cerrar el tablero sea moler una A. En los niveles
        // con receta se arranca con una hierba ya molida dentro: como la meta
        // exige el molinillo vacio, hay que usarlo para sacarla.
        if (grinderIndex >= 0) {
            if (requiredOrder == null) {
                if (!crearDesequilibrio(stacks, availableHerbs, teapotIndex, grinderIndex, random)) {
                    return null
                }
            } else if (!sembrarMolinillo(stacks, availableHerbs, teapotIndex, grinderIndex, random)) {
                return null
            }
        }

        val marcas = elegirMarcas(
            stacks = stacks,
            teapotIndex = teapotIndex,
            grinderIndex = grinderIndex,
            levelNumber = levelNumber,
            sector = sector,
            misterioExtra = adorno.misterioExtra,
            random = random
        ) ?: return null

        return TableroPreparado(stacks, teapotIndex, grinderIndex, marcas)
    }

    /**
     * Cambia una capa de B por una de A, con A.molida == B. El tablero queda con
     * una A de mas y una B de menos: sin pasar una A por el molinillo no hay
     * forma de tener las cuatro B que pide su frasco.
     */
    private fun crearDesequilibrio(
        stacks: MutableList<MutableList<Herb>>,
        availableHerbs: List<Herb>,
        teapotIndex: Int,
        grinderIndex: Int,
        random: Random
    ): Boolean {
        val pares = Herb.delReparto
            .filter { it in availableHerbs && it.molida in availableHerbs }
            .map { it to it.molida }
            .shuffled(random)
        for ((sobra, falta) in pares) {
            val posiciones = mutableListOf<Pair<Int, Int>>()
            for (i in stacks.indices) {
                if (i == teapotIndex || i == grinderIndex) continue
                val frasco = stacks[i]
                // No se mete una hierba repetida en el mismo frasco: se rompe la
                // regla de "nada de rojo rojo mora mora" y ademas es innecesario.
                if (frasco.any { it == sobra }) continue
                for (j in frasco.indices) {
                    if (frasco[j] != falta) continue
                    val izquierda = if (j > 0) frasco[j - 1] else null
                    val derecha = if (j < frasco.lastIndex) frasco[j + 1] else null
                    if (izquierda != sobra && derecha != sobra) posiciones.add(i to j)
                }
            }
            if (posiciones.isNotEmpty()) {
                val (i, j) = posiciones.random(random)
                stacks[i][j] = sobra
                return true
            }
        }
        return false
    }

    /**
     * Arranca el molinillo con una capa ya molida. La capa sale de la cima de un
     * frasco normal (el tablero conserva todas las capas, solo cambia una de
     * sitio y de color) y como la meta exige el molinillo vacio, hay que
     * devolverla a un frasco para terminar.
     *
     * La hierba molida tiene que existir ya en el nivel: si el molinillo
     * escupiera la siguiente del ciclo completo (la molida de calendula es
     * tomillo), el nivel traeria una especie de mas que la ficha del mapa no
     * anuncia.
     */
    private fun sembrarMolinillo(
        stacks: MutableList<MutableList<Herb>>,
        availableHerbs: List<Herb>,
        teapotIndex: Int,
        grinderIndex: Int,
        random: Random
    ): Boolean {
        val candidatos = stacks.indices.filter {
            it != teapotIndex && it != grinderIndex &&
                stacks[it].isNotEmpty() && stacks[it].last().molida in availableHerbs
        }
        if (candidatos.isEmpty()) return false
        val donante = candidatos.random(random)
        val capa = stacks[donante].removeAt(stacks[donante].lastIndex)
        stacks[grinderIndex].add(capa.molida)
        return true
    }

    private fun elegirMarcas(
        stacks: List<MutableList<Herb>>,
        teapotIndex: Int,
        grinderIndex: Int,
        levelNumber: Int,
        sector: SectorRule,
        misterioExtra: Boolean,
        random: Random
    ): Marcas? {
        val normales = stacks.indices.filter {
            it != teapotIndex && it != grinderIndex && stacks[it].isNotEmpty()
        }
        val conInterior = normales.filter { stacks[it].size >= 2 }
        if (levelNumber >= 3 && conInterior.isEmpty()) return null

        // Un frasco helado esta bloqueado entero hasta que se complete otro.
        // Se elige uno con varias capas y, a ser posible, distinto del de la
        // miel para que las dos mecanicas se lean por separado.
        val helado = if (sector.traeHelados) {
            (conInterior.ifEmpty { normales }).random(random)
        } else {
            null
        }

        // La miel va en el FONDO del frasco. Si estuviera en medio, las capas de
        // debajo quedarian atrapadas para siempre (la miel no se puede sacar) y
        // el frasco no se podria ordenar. En el fondo, el frasco se termina
        // cubriendo la miel con su mismo color: el frasco acaba de ese color.
        val pegajosas = mutableSetOf<Pair<Int, Int>>()
        var frascoPegajoso = -1
        if (sector.traePegajosas) {
            val candidatos = conInterior.filter { it != helado }.ifEmpty { conInterior }
            if (candidatos.isEmpty()) return null
            frascoPegajoso = candidatos.random(random)
            pegajosas.add(frascoPegajoso to 0)
        }

        val misterios = mutableSetOf<Pair<Int, Int>>()
        // El misterio esta garantizado desde su estreno: la ficha del nivel no
        // puede decir "capas misteriosas" y luego no traer ninguna.
        if (levelNumber >= 3) {
            val candidatosMisterio = conInterior.filter { jar ->
                stacks[jar].indices.any { capa -> (jar to capa) !in pegajosas }
            }
            if (candidatosMisterio.isEmpty()) return null
            val jar = candidatosMisterio.random(random)
            val capasPosibles = stacks[jar].indices.filter { capa -> (jar to capa) !in pegajosas }
            val capa = capasPosibles.random(random)
            misterios.add(jar to capa)
        }
        if (misterioExtra && levelNumber >= 3) {
            conInterior.forEach { jar ->
                for (capa in 0 until stacks[jar].size - 1) {
                    if ((jar to capa) in pegajosas) continue
                    if (random.nextFloat() < 0.18f) misterios.add(jar to capa)
                }
            }
        }

        return Marcas(misterios, helado, pegajosas)
    }

    /** Convierte el tablero preparado en los frascos exactos que ve el motor. */
    private fun aJars(
        preparado: TableroPreparado,
        jarCapacity: Int,
        requiredOrder: List<Herb>?
    ): List<Jar> = preparado.stacks.mapIndexed { index, capas ->
        Jar(
            id = index,
            capacity = when {
                index == preparado.grinderIndex -> 1
                index == preparado.teapotIndex ->
                    requiredOrder?.size?.coerceAtLeast(1) ?: jarCapacity
                else -> jarCapacity
            },
            layers = capas.mapIndexed { capa, herb ->
                JarLayer(
                    herb = herb,
                    isHidden = herb.esSecreta || (index to capa) in preparado.marcas.misterio,
                    isFrozen = index == preparado.marcas.helado,
                    isSticky = (index to capa) in preparado.marcas.pegajosa
                )
            },
            isTeapot = index == preparado.teapotIndex,
            isGrinder = index == preparado.grinderIndex
        ).revealTopLayer()
    }

    private fun nivelDesde(
        levelNumber: Int,
        jars: List<Jar>,
        preparado: TableroPreparado,
        solucion: List<Pair<Int, Int>>
    ): GameLevel = GameLevel(
        levelNumber = levelNumber,
        jarCapacity = jars.firstOrNull { !it.isTeapot && !it.isGrinder }?.capacity ?: 4,
        jars = jars.map { jar -> jar.layers.map { it.idParaElNivel() } },
        parMoves = solucion.size,
        sectorRule = SectorRule.forLevel(levelNumber),
        grinderJarIndex = preparado.grinderIndex.takeIf { it >= 0 }
    )

    /**
     * Baraja las capas de un frasco sin dejar dos iguales pegadas, que es lo que
     * se ve mal: "rojo rojo mora mora" en vez de "rojo mora verde otro".
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

    /**
     * Nivel de reserva. Antes se devolvia un tablero sin las mecanicas del
     * sector; ahora se usa el mismo preparado que el camino normal y se valida
     * varias veces. Solo si no hay forma se entrega un tablero trivial (todavia
     * jugable, que es lo importante para una puerta).
     */
    private fun createFallbackLevel(
        levelNumber: Int,
        herbs: List<Herb>,
        jarCapacity: Int,
        emptyJars: Int,
        requiredOrder: List<Herb>?,
        secreto: Herb?
    ): GameLevel {
        val sector = SectorRule.forLevel(levelNumber)
        // El recorte nunca deja fuera la receta: si el nivel pide hibisco y el
        // recorte se lo lleva, el nivel no se puede servir de ninguna manera.
        fun recorte(cuantas: Int): List<Herb> {
            val deReceta = requiredOrder?.distinct().orEmpty()
            val resto = herbs.filterNot { it in deReceta }
            return (resto.take(cuantas) + deReceta).distinct()
        }
        val candidates = buildList {
            add(herbs)
            recorte(4).let { if (it.isNotEmpty() && it != herbs) add(it) }
            recorte(3).let { if (it.isNotEmpty() && it !in this) add(it) }
            recorte(2).let { if (it.isNotEmpty() && it !in this) add(it) }
            requiredOrder?.distinct()?.let { if (it.isNotEmpty() && it !in this) add(it) }
        }

        for (candidate in candidates) {
            for (intento in 0 until 12) {
                val rnd = Random(levelNumber * 31 + candidate.size + intento * 977)
                val extra = if (requiredOrder != null) 1 else 0
                val teapotIndex = if (extra == 1) candidate.size + emptyJars else -1
                val base = repartirCapas(
                    herbs = candidate,
                    jarCapacity = jarCapacity,
                    emptyJars = emptyJars + extra,
                    teapotIndex = teapotIndex,
                    fraccionMezcla = 0.0,
                    random = rnd
                )
                val preparado = prepararTablero(
                    base = base,
                    teapotIndex = if (extra == 1) base.lastIndex else -1,
                    conMolinillo = sector.traeMolinillo,
                    requiredOrder = requiredOrder,
                    availableHerbs = candidate,
                    secreto = secreto,
                    levelNumber = levelNumber,
                    sector = sector,
                    adorno = Adorno(0.0, misterioExtra = false),
                    random = rnd
                ) ?: continue
                val jars = aJars(preparado, jarCapacity, requiredOrder)
                val solucion = solver.repartirYResolver(jars, requiredOrder) ?: continue
                return nivelDesde(levelNumber, jars, preparado, solucion)
            }
        }

        // Ultimo recurso. Antes se devolvia el candidato sin comprobar, y eso
        // podia dejar un nivel de puerta irresoluble. Un tablero trivial no
        // ensena las mecanicas del sector, pero al menos se puede terminar.
        return nivelTrivial(levelNumber, herbs, jarCapacity, emptyJars, requiredOrder, sector)
    }

    private fun nivelTrivial(
        levelNumber: Int,
        herbs: List<Herb>,
        jarCapacity: Int,
        emptyJars: Int,
        requiredOrder: List<Herb>?,
        sector: SectorRule
    ): GameLevel {
        val elegidas = requiredOrder?.distinct()?.takeIf { it.isNotEmpty() }
            ?: herbs.take(1).ifEmpty { listOf(Herb.CHAMOMILE) }
        val stacks = elegidas.map { herb ->
            MutableList(jarCapacity) { herb }
        }.toMutableList()
        repeat(emptyJars + if (requiredOrder != null) 1 else 0) {
            stacks.add(mutableListOf())
        }
        val teapotIndex = if (requiredOrder != null) stacks.lastIndex else -1
        val grinderIndex = if (sector.traeMolinillo) {
            stacks.add(mutableListOf())
            stacks.lastIndex
        } else {
            -1
        }
        val preparado = TableroPreparado(
            stacks = stacks,
            teapotIndex = teapotIndex,
            grinderIndex = grinderIndex,
            marcas = Marcas(emptySet(), null, emptySet())
        )
        val jars = aJars(preparado, jarCapacity, requiredOrder)
        return GameLevel(
            levelNumber = levelNumber,
            jarCapacity = jarCapacity,
            jars = jars.map { jar -> jar.layers.map { it.idParaElNivel() } },
            parMoves = solver.repartirYResolver(jars, requiredOrder)?.size,
            sectorRule = sector,
            grinderJarIndex = preparado.grinderIndex.takeIf { it >= 0 }
        )
    }

    /** La capa tal y como la lee [LevelRepository.convertToJars]. */
    private fun JarLayer.idParaElNivel(): String = buildString {
        // El orden importa: el repositorio quita primero "frozen:", luego
        // "sticky:" y por ultimo el "?" del misterio.
        if (isFrozen) append("frozen:")
        if (isSticky) append("sticky:")
        if (isHidden) append("?")
        append(herb.id)
    }
}
