package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import java.util.ArrayDeque
import kotlin.random.Random

/**
 * Resuelve niveles de frascos.
 *
 * Con tetera (niveles con receta) el solve va por fases, porque el espacio de
 * estados completo es enorme para una BFS:
 *  1. Fase 1: servir la receta en la tetera. Como la tetera admite cualquier
 *     hierba, se avanza slot a slot: en cada paso se busca (con una BFS corta)
 *     el menor conjunto de movimientos que hace crecer la tetera manteniendo
 *     un prefijo válido de la receta.
 *  2. Fase 2: dejar el resto de frascos vacíos o puros.
 *
 * El solver replica exactamente las reglas de GameEngine.pour / executePour,
 * incluidas las capas misteriosas (una capa oculta corta la ráfaga) y los
 * sellos de cera (se abren según los frascos completados). Por eso la Pista
 * nunca sugiere una jugada ilegal.
 */
class LevelSolver {

    /** Capa dentro de un frasco, con su estado de "misterio". */
    data class Slot(
        val herb: Herb,
        val hidden: Boolean = false,
        val sticky: Boolean = false,
        val frozen: Boolean = false
    )

    data class State(
        val jars: List<List<Slot>>,
        val sealed: Set<Int>,
        /**
         * Frasco que hace de molinillo, si el nivel trae uno. La busqueda lo
         * modela como una herramienta de una sola capa: puede recibir cualquier
         * hierba (y la transforma en [Herb.molida]) y puede soltar lo que tiene.
         */
        val grinder: Int? = null
    )

    private data class Step(val moves: List<Pair<Int, Int>>, val state: State)

    /**
     * Cuántos estados puede visitar la búsqueda antes de rendirse.
     *
     * Cada estado guarda los frascos enteros, así que el presupuesto es también
     * un tope de memoria. Medido sobre el nivel 39 (8 hierbas, 10 frascos) con
     * barajados de 30 vertidos: 60000 estados dan 3/3 resueltos con unos 60 MB.
     * Con 400000, que era el valor anterior, la busqueda llegaba a 190 MB y en un
     * móvil (heap de 192 MB) el nivel 39 terminaba en OutOfMemoryError y se
     * caía la partida. Menos nodos, menos apuros.
     */
    companion object {
        private const val DEFAULT_MAX_VISITED = 60_000

        /** Tope de vertidos del reparto sin busqueda, para que no se ciclie. */
        private const val MAX_PASOS_REPARTO = 600

        /**
         * Formas de recorrer los frascos en el reparto: en orden, al reves y dos
         * barajados. El reparto es voraz, prueba la primera jugada que ve, y a
         * veces se encierra; con otro orden la misma partida sale.
         */
        private const val ESTRATEGIAS_REPARTO = 7
    }

    fun isSolvable(initialJars: List<Jar>, requiredOrder: List<Herb>? = null): Boolean {
        return solve(initialJars, requiredOrder) != null
    }

    /**
     * Resuelve el nivel vertiendo, sin buscar: elige siempre el movimiento mas
     * obvio (bloque superior a un frasco del mismo color y con hueco) y, si no
     * cabe, a un frasco vacio.
     *
     * Existe por dos razones. Para comprobar niveles: la busqueda por anchura
     * tiene que recorrer decenas de miles de estados y con los niveles bien
     * repartidos (cuatro hierbas distintas en cada frasco) ya no llegaba, se
     * comia la memoria y el generador se colgaba. Y para la Pista: este mismo
     * reparto devuelve movimientos validos, asi que la pista funciona tambien en
     * los niveles donde la busqueda se quedaba corta.
     *
     * Si dice que si, el nivel es ganable de verdad: los movimientos que hace son
     * legales y acaban con todos los frascos ordenados. Puede decir que no en un
     * nivel que otro camino si resolveria; por eso el generador reintenta con
     * otro reparto en vez de quedarse con el primero que se le ocurra.
     */
    fun repartirYResolver(
        initialJars: List<Jar>,
        requiredOrder: List<Herb>? = null
    ): List<Pair<Int, Int>>? {
        val teapotIndex = initialJars.indexOfFirst { it.isTeapot }.takeIf { it >= 0 }
        val recipe = requiredOrder?.takeIf { it.isNotEmpty() }
        val capacity = initialJars.filterNot { it.isTeapot || it.isGrinder }
            .maxOfOrNull { it.capacity } ?: 4
        val teapotCapacity = teapotIndex?.let { initialJars[it].capacity } ?: capacity

        // Las capas con su estado: no hace falta saber cuales estan escondidas, porque
        // el motor destapa la cima de origen y destino tras cada vertido. Se
        // guardan las marcas de pegajosa y helado, que el reparto tiene que
        // respetar igual que el motor.
        val frascosIniciales = initialJars.map { jar -> jar.layers.toMutableList() }.toMutableList()
        var frascos: MutableList<MutableList<JarLayer>> = frascosIniciales.map { it.toMutableList() }.toMutableList()
        // Un frasco sellado no se puede tocar hasta que se abren otros completando
        // frascos, igual que hace el motor: se van abriendo sobre la marcha.
        val selladosIniciales = initialJars.map { it.isSealed }
        var sellados = selladosIniciales.toMutableList()
        var movimientos = mutableListOf<Pair<Int, Int>>()

        // El molinillo es el unico frasco que no es un frasco: guarda una sola
        // hierba y la transforma. La meta exige que quede vacio, y hay niveles
        // construidos con una hierba de mas y otra de menos: el reparto detecta
        // ese desequilibrio y muele la que sobra para convertirla en la que
        // falta. Sin ese plan, la partida no se podria cerrar.
        val grinderIndex = initialJars.indexOfFirst { it.isGrinder }.takeIf { it >= 0 }

        fun limiteDe(indice: Int): Int = when (indice) {
            teapotIndex -> teapotCapacity
            grinderIndex -> 1
            else -> capacity
        }

        // Igual que el motor: de un frasco sellado, helado o con una capa
        // pegajosa en la cima no se puede verter.
        fun jugables(): List<Int> =
            frascos.indices.filter {
                it != teapotIndex && !sellados[it] && frascos[it].isNotEmpty() &&
                    frascos[it].none { capa -> capa.isFrozen } &&
                    frascos[it].last().let { cima -> !cima.isHidden && !cima.isSticky }
            }

        /** Destinos validos para ordenar: ni la tetera, ni el molinillo, ni un sellado. */
        fun destinos(): List<Int> = frascos.indices.filter {
            it != teapotIndex && it != grinderIndex && !sellados[it]
        }

        fun bloqueSuperior(indice: Int): Int {
            val frasco = frascos[indice]
            if (frasco.isEmpty()) return 0
            // Frasco helado o cima escondida/pegajosa: el motor no deja verter
            // de ahi, cuenta como cero.
            if (frasco.any { capa -> capa.isFrozen }) return 0
            if (frasco.last().isHidden || frasco.last().isSticky) return 0
            val cima = frasco.last().herb
            var n = 0
            for (k in frasco.indices.reversed()) {
                // La capa pegajosa o escondida corta el bloque, igual que en el
                // motor: solo sale lo que hay por encima del tope.
                val tope = frasco[k].isSticky || frasco[k].isHidden
                if (k != frasco.lastIndex && tope) break
                if (frasco[k].herb == cima) n++ else break
            }
            return n
        }

        // Meta del motor, tal cual: con receta el nivel se gana sirviendola
        // ([GameEngine.isOrderFulfilled] pide la tetera con la receta y el resto
        // vacio o de un solo color) y sin receta hay que ordenar los frascos
        // ([Jar.isCompleted]).
        fun terminado(indice: Int): Boolean {
            // El molinillo no almacena: para dar el nivel por bueno tiene que
            // quedar vacio, como la tetera tiene que cuadrar con la receta.
            if (indice == grinderIndex) return frascos[indice].isEmpty()
            if (indice == teapotIndex) {
                return recipe == null || frascos[indice].map { it.herb } == recipe
            }
            val frasco = frascos[indice]
            if (frasco.isEmpty()) return true
            // Un frasco sellado o helado no cuenta como terminado.
            if (sellados[indice]) return false
            if (frasco.any { it.isFrozen }) return false
            if (frasco.map { it.herb }.distinct().size > 1) return false
            // Con receta el motor admite el frasco a medias mientras sea de una
            // sola hierba. Sin receta hay que llenarlo, y sin capas por destapar,
            // porque [Jar.isCompleted] tambien las mira.
            if (recipe != null) return true
            return frasco.size == limiteDe(indice) &&
                frasco.all { !it.isHidden }
        }

        fun completo(indice: Int): Boolean {
            if (indice == teapotIndex || indice == grinderIndex) return false
            val frasco = frascos[indice]
            return frasco.size == limiteDe(indice) &&
                frasco.map { it.herb }.distinct().size == 1
        }

        fun abrirSellados() {
            // El motor solo abre un frasco sellado por cada frasco que queda
            // COMPLETO: lleno y de una sola hierba. Con receta un frasco puro a
            // medias cuenta como ordenado pero no abre sellos, asi que aqui no
            // vale la regla floja de [terminado] o el reparto daria jugadas
            // que el motor rechaza.
            val completos = frascos.indices.count { it != teapotIndex && completo(it) }
            var porAbrir = completos
            for (i in sellados.indices) {
                if (porAbrir > 0 && sellados[i]) {
                    sellados[i] = false
                    porAbrir--
                }
            }
        }

        /** Al completar un frasco se rompe todo el hielo del tablero. */
        fun romperHielo() {
            if (frascos.indices.none { completo(it) }) return
            frascos.indices.forEach { i ->
                if (frascos[i].any { it.isFrozen }) {
                    frascos[i] = frascos[i]
                        .map { if (it.isFrozen) it.copy(isFrozen = false) else it }
                        .toMutableList()
                }
            }
        }

        /** Destapa la cima, como hace el motor al verter. */
        fun destaparLaCima(indice: Int) {
            val frasco = frascos[indice]
            if (frasco.isEmpty()) return
            val cima = frasco.last()
            if (cima.isHidden) {
                frascos[indice][frasco.lastIndex] = cima.copy(isHidden = false)
            }
        }

        fun verter(desde: Int, hacia: Int, cuanto: Int) {
            repeat(cuanto) {
                var capa = frascos[desde].removeAt(frascos[desde].lastIndex)
                if (hacia == grinderIndex) capa = capa.copy(herb = capa.herb.molida)
                frascos[hacia].add(capa)
            }
            // El motor destapa la cima nueva de los dos frascos despues de cada
            // vertido. Sin esto el reparto dejaba frascos con la cima tapada y se
            // quedaba sin jugadas donde el motor seguiria teniendo.
            destaparLaCima(desde)
            destaparLaCima(hacia)
            movimientos.add(desde to hacia)
            romperHielo()
            abrirSellados()
        }

        /**
         * Hierba que sobra y hierba que falta en un nivel con molinillo. El
         * generador crea ese desequilibrio a proposito (5 de A, 3 de B, con
         * A.molida == B), y el reparto tiene que moler una A para cerrar el
         * tablero. Sin esto el molinillo seria decorativo.
         */
        fun planDeMolienda(): Pair<Herb, Herb>? {
            if (recipe != null || grinderIndex == null) return null
            val cuenta = mutableMapOf<Herb, Int>()
            frascos.forEachIndexed { i, capas ->
                if (i == teapotIndex || i == grinderIndex) return@forEachIndexed
                capas.forEach { capa -> cuenta[capa.herb] = (cuenta[capa.herb] ?: 0) + 1 }
            }
            return Herb.delReparto.firstNotNullOfOrNull { sobra ->
                val falta = sobra.molida
                val deSobra = cuenta[sobra] ?: 0
                val deFalta = cuenta[falta] ?: 0
                if (deSobra > 0 && deFalta > 0 &&
                    deSobra % capacity == 1 && deFalta % capacity == capacity - 1
                ) sobra to falta else null
            }
        }

        val planInicial = planDeMolienda()
        var planPendiente = planInicial
        // El motor descongela al cargar si ya hay un frasco completo (raro, pero
        // el nivel de reserva podria traerlo).
        romperHielo()

        // Manera de recorrer los frascos en esta pasada: en orden, al reves o
        // barajados. El reparto prueba la primera jugada que ve, asi que el orden
        // decide si sale del atolladero o no.
        var estrategia = 0
        fun enOrden(indices: List<Int>): List<Int> = when (estrategia) {
            0 -> indices
            1 -> indices.reversed()
            2, 3 -> indices.shuffled(Random(estrategia.toLong() * 7919L))
            else -> indices
        }

        // Y entre los destinos validos tambien hay donde elegir: el de mas sitio
        // (para no desperdiciar el bloque) o el de menos (para rematar el frasco).
        fun elegirDestino(opciones: List<Int>): Int? = when (estrategia) {
            4 -> opciones.maxByOrNull { limiteDe(it) - frascos[it].size }
            5 -> opciones.minByOrNull { limiteDe(it) - frascos[it].size }
            else -> opciones.firstOrNull()
        }

        // La ultima estrategia ordena todo antes de servir la tetera, que es como
        // lo hace un jugador que va sobre seguro.
        fun servirYa(): Boolean = estrategia != 6 ||
            frascos.indices.all { it == teapotIndex || terminado(it) }

        /** Cuantas capas seguidas de [herb] hay en la cima del frasco. */
        fun cuantasArriba(indice: Int, herb: Herb): Int {
            var n = 0
            for (k in frascos[indice].indices.reversed()) {
                if (frascos[indice][k].herb == herb) n++ else break
            }
            return n
        }

        /**
         * Entre los frascos que ya tienen esa hierba arriba, el que mas capas
         * lleva: consolidar en el frasco casi hecho es lo que hace un jugador y
         * evita ir moviendo el mismo bloque de un lado a otro.
         */
        fun mejorDestinoMismoColor(opciones: List<Int>, herb: Herb): Int? =
            opciones.maxWithOrNull(
                compareBy({ cuantasArriba(it, herb) }, { limiteDe(it) - frascos[it].size })
            )

        /**
         * El molinillo no es almacen: si tiene algo dentro hay que devolverlo lo
         * antes posible. Si se deja para el final, el reparto gasta los frascos
         * vacios ordenando y el molinillo se queda sin sitio (el nivel parecia
         * imposible con salida).
         */
        fun vaciarMolinillo(): Boolean {
            val g = grinderIndex ?: return false
            val capas = frascos[g]
            if (capas.isEmpty()) return false
            val cima = capas.last()
            if (cima.isHidden || cima.isSticky) return false
            if (capas.any { it.isFrozen }) return false
            val herb = cima.herb
            val mismos = destinos().filter { j ->
                j != g && frascos[j].isNotEmpty() && frascos[j].last().herb == herb &&
                    frascos[j].size < limiteDe(j)
            }
            val destino = mejorDestinoMismoColor(mismos, herb)
                ?: elegirDestino(
                    enOrden(destinos()).filter { j -> j != g && frascos[j].isEmpty() }
                )
                ?: return false
            verter(g, destino, 1)
            return true
        }

        var pasos = 0
        var intento = 0
        // Guarda contra dar vueltas: el reparto es voraz y no mira atras, asi que
        // puede mecer el mismo bloque de un frasco a otro las 600 jugadas y darse
        // por vencido con un nivel que tiene salida (y encima sin llegar a probar
        // los otros ordenes, porque el bucle acababa por presupuesto). Si vuelve a
        // un estado que ya ha visto, se trata como atasco y se cambia de orden.
        val vistos = mutableSetOf<String>()
        fun firma(): String = buildString {
            frascos.forEach { frasco ->
                frasco.forEach { capa ->
                    append(capa.herb.id)
                    if (capa.isHidden) append('?')
                    if (capa.isFrozen) append('f')
                    if (capa.isSticky) append('s')
                    append('.')
                }
                append('|')
            }
            sellados.forEach { if (it) append('X') }
        }
        while (pasos++ < MAX_PASOS_REPARTO) {
            // Meta del motor: con receta, la tetera servida y el resto vacio o de
            // un color; sin receta, todos los frascos ordenados.
            if (frascos.indices.all { terminado(it) }) return movimientos

            if (!vistos.add(firma())) {
                intento++
                if (intento >= ESTRATEGIAS_REPARTO) return null
                estrategia = intento
                frascos = frascosIniciales.map { it.toMutableList() }.toMutableList()
                sellados = selladosIniciales.toMutableList()
                movimientos = mutableListOf()
                planPendiente = planInicial
                romperHielo()
                pasos = 0
                vistos.clear()
                continue
            }

            val libres = enOrden(jugables())
            var hecho = false

            // Moler es obligatorio en los niveles con desequilibrio: hay mas A
            // que B y A.molida == B. Se hace en cuanto la A esta en la cima.
            val plan = planPendiente
            if (!hecho && plan != null && grinderIndex != null &&
                frascos[grinderIndex].isEmpty()
            ) {
                val origen = libres.firstOrNull { frascos[it].lastOrNull()?.herb == plan.first }
                if (origen != null) {
                    verter(origen, grinderIndex, 1)
                    planPendiente = null
                    hecho = true
                }
            }

            if (!hecho && vaciarMolinillo()) {
                hecho = true
            }

            if (!hecho && teapotIndex != null && recipe != null && frascos[teapotIndex].size < recipe.size &&
                servirYa()
            ) {
                // Se sirve la tetera en cuanto la hierba que toca esta arriba en
                // algun frasco; si no la hay, se ordena hasta que salga. Servirla
                // pronto es lo que hace el jugador, y asi el par cuenta lo que
                // cuesta ganar de verdad.
                // Con la tetera ya llena no se entra aqui: el nivel sigue sin
                // estar terminado (queda ordenar el resto) y lo que toca es
                // ordenar, no dar la partida por imposible.
                val tetera = frascos[teapotIndex]
                // Si la tetera lleva algo que no es la receta no hay nada que
                // servir: no existe la hierba que le toca.
                if (tetera.map { it.herb } != recipe.take(tetera.size)) return null
                val falta = recipe[tetera.size]
                val origen = libres.firstOrNull { frascos[it].lastOrNull()?.herb == falta }
                if (origen != null) {
                    verter(origen, teapotIndex, 1)
                    hecho = true
                }
            }
            if (!hecho) {
                // Ordenar: bloque superior a un frasco del mismo color con sitio,
                // y si no hay sitio a un frasco vacio.
                for (i in libres) {
                    val cuanto = bloqueSuperior(i)
                    if (cuanto == 0) continue
                    val cima = frascos[i].last().herb
                    val candidatos = enOrden(destinos()).filter { j ->
                        j != i && frascos[j].isNotEmpty() && frascos[j].last().herb == cima &&
                            frascos[j].size < limiteDe(j)
                    }
                    val destino = if (estrategia == 4 || estrategia == 5) {
                        elegirDestino(candidatos)
                    } else {
                        mejorDestinoMismoColor(candidatos, cima)
                    }
                    if (destino != null) {
                        // Como el motor, que vierte lo que cabe en el destino aunque
                        // el bloque sea mayor. Antes el reparto exigia que cupiera
                        // entero y con cuatro colores se quedaba sin jugadas en un
                        // callejon sin salida: un frasco con 3 de un color y un bloque
                        // de 2 del mismo, y ningun sitio donde meterlo.
                        val cabe = minOf(cuanto, limiteDe(destino) - frascos[destino].size)
                        verter(i, destino, cabe)
                        hecho = true
                        break
                    }
                }
                if (!hecho) {
                    // Sin destino del mismo color: se hace hueco bajando el
                    // bloque mas grande de un frasco mezclado. Mover un frasco que
                    // ya es de un solo color a un vacio no arregla nada y gasta el
                    // hueco, asi que se salta.
                    val fuentes = libres
                        .filter { i ->
                            bloqueSuperior(i) > 0 &&
                                frascos[i].map { it.herb }.distinct().size > 1
                        }
                        .sortedByDescending { bloqueSuperior(it) }
                    for (i in fuentes) {
                        val cuanto = bloqueSuperior(i)
                        val destino = elegirDestino(
                            enOrden(destinos()).filter { j -> j != i && frascos[j].isEmpty() }
                        )
                        if (destino != null) {
                            verter(i, destino, cuanto)
                            hecho = true
                            break
                        }
                    }
                }
                if (!hecho && grinderIndex != null && frascos[grinderIndex].isEmpty()) {
                    // Ultimo recurso: el molinillo tambien sirve de salida. Echar
                    // una capa que no tiene destino normal la muele, y si la
                    // molida si tiene sitio, la jugada desatasca el tablero (es
                    // justo lo que hace un jugador con la hierba secreta).
                    for (i in libres) {
                        val cuanto = bloqueSuperior(i)
                        if (cuanto == 0) continue
                        val molida = frascos[i].last().herb.molida
                        val donde = destinos().any { j ->
                            j != i && frascos[j].size < limiteDe(j) &&
                                (frascos[j].isEmpty() || frascos[j].last().herb == molida)
                        }
                        if (donde) {
                            verter(i, grinderIndex, 1)
                            hecho = true
                            break
                        }
                    }
                }
            }
            if (!hecho) {
                // Atascado: se empieza de nuevo con otro orden de recorrido. La
                // misma partida con otro orden sale, y rendirse a la primera era
                // lo que dejaba al generador sin niveles con receta en el bosque
                // (entregaba el nivel de reserva, mucho mas plano).
                intento++
                if (intento >= ESTRATEGIAS_REPARTO) return null
                estrategia = intento
                frascos = frascosIniciales.map { it.toMutableList() }.toMutableList()
                sellados = selladosIniciales.toMutableList()
                movimientos = mutableListOf()
                planPendiente = planInicial
                romperHielo()
                pasos = 0
            }
        }
        return null
    }

    /** Comprobacion rapida de que el nivel se puede terminar. Ver [repartirYResolver]. */
    fun esGanalible(initialJars: List<Jar>, requiredOrder: List<Herb>? = null): Boolean =
        repartirYResolver(initialJars, requiredOrder) != null

    /**
     * Como [isSolvable], pero exige que la busqueda haya encontrado la solucion
     * de verdad y no se haya quedado corta.
     *
     * Importa porque la busqueda se corta por presupuesto: un null no demuestra
     * que el nivel sea imposible, solo que no se ha explorado lo suficiente. Un
     * nivel puerta que solo se comprobo a medias puede dejar al jugador
     * bloqueado sin poder desbloquear la hierba, asi que para darlo por bueno
     * hace falta haberlo resuelto.
     */
    fun isVerifiedSolvable(initialJars: List<Jar>, requiredOrder: List<Herb>? = null): Boolean {
        return solve(initialJars, requiredOrder) != null
    }

    /**
     * Resuelve el nivel. Primero por busqueda, que encuentra las jugadas mas
     * cortas, y si no llega al fondo cae en el reparto sin busqueda
     * ([repartirYResolver]): con los niveles bien dispersos la busqueda se queda
     * sin presupuesto a mitad y daria un "imposible" en niveles que si se
     * terminan.
     */
    fun solve(
        initialJars: List<Jar>,
        requiredOrder: List<Herb>? = null,
        maxVisited: Int = DEFAULT_MAX_VISITED
    ): List<Pair<Int, Int>>? =
        buscarPorAnchura(initialJars, requiredOrder, maxVisited)
            ?: repartirYResolver(initialJars, requiredOrder)

    private fun buscarPorAnchura(
        initialJars: List<Jar>,
        requiredOrder: List<Herb>? = null,
        maxVisited: Int = DEFAULT_MAX_VISITED
    ): List<Pair<Int, Int>>? {
        val teapotIndex = initialJars.indexOfFirst { it.isTeapot }.takeIf { it >= 0 }
        val recipe = requiredOrder?.takeIf { it.isNotEmpty() }
        val capacity = initialJars.filterNot { it.isTeapot || it.isGrinder }
            .maxOfOrNull { it.capacity }
            ?: initialJars.maxOfOrNull { it.capacity }
            ?: 4
        val teapotCapacity = teapotIndex?.let { initialJars[it].capacity } ?: capacity
        val grinderIndex = initialJars.indexOfFirst { it.isGrinder }.takeIf { it >= 0 }
        val start = State(
            jars = initialJars.map { jar ->
                jar.layers.map { Slot(it.herb, it.isHidden, it.isSticky, it.isFrozen) }
            },
            sealed = initialJars.indices.filter { initialJars[it].isSealed }.toSet(),
            grinder = grinderIndex
        )

        if (teapotIndex != null && recipe != null) {
            return solveWithTeapot(start, recipe, teapotIndex, capacity, teapotCapacity, maxVisited)
        }

        return solveBySearch(start, requiredOrder, capacity, teapotCapacity, maxVisited)
    }

    // -----------------------------------------------------------------------
    // Fase 1 + 2: receta en la tetera y orden del resto
    // -----------------------------------------------------------------------

    private fun solveWithTeapot(
        start: State,
        recipe: List<Herb>,
        teapotIndex: Int,
        capacity: Int,
        teapotCapacity: Int,
        maxVisited: Int
    ): List<Pair<Int, Int>>? {
        var state = start
        val moves = mutableListOf<Pair<Int, Int>>()
        var guard = 0

        // La receta se sirve slot a slot, asi que la fase 1 se llama varias
        // veces. Si cada llamada gastara el presupuesto entero, las ultimas
        // (y la ordenacion final) se quedarian sin nada que explorar, y el
        // solver daria el nivel por imposible siendo jugable. Se reparte.
        val recipeSlots = maxOf(recipe.size, 1)
        val perSlotBudget = maxOf(maxVisited / (recipeSlots + 1), 30_000)

        while (state.jars[teapotIndex].herbs() != recipe) {
            if (++guard > 16) return null
            val current = state.jars[teapotIndex].herbs()
            // Si ya no es un prefijo de la receta, la partida es irrecuperable.
            if (current.size > recipe.size) return null
            if (current != recipe.take(current.size)) return null

            val step = findProgressStep(state, recipe, teapotIndex, capacity, teapotCapacity, perSlotBudget)
                ?: return null
            moves.addAll(step.moves)
            state = step.state
        }

        val restIsSorted = { s: State ->
            s.jars.indices.all { i ->
                when {
                    i == teapotIndex -> true
                    i == s.grinder -> s.jars[i].isEmpty()
                    s.jars[i].isEmpty() -> true
                    else -> s.jars[i].herbs().distinct().size == 1 &&
                        s.jars[i].none { it.frozen }
                }
            }
        }
        if (!restIsSorted(state)) {
            val restMoves = solvePureJars(state, teapotIndex, capacity, teapotCapacity, maxVisited, restIsSorted)
                ?: return null
            moves.addAll(restMoves)
        }

        return moves
    }

    /**
     * Menor secuencia de movimientos que hace crecer la tetera manteniendo un
     * prefijo válido de la receta.
     */
    private fun findProgressStep(
        start: State,
        recipe: List<Herb>,
        teapotIndex: Int,
        capacity: Int,
        teapotCapacity: Int,
        maxVisited: Int
    ): Step? {
        val startSize = start.jars[teapotIndex].size
        val isBetter = { s: State ->
            val teapot = s.jars[teapotIndex].herbs()
            teapot.size > startSize && teapot == recipe.take(teapot.size)
        }
        if (isBetter(start)) return Step(emptyList(), start)

        // Esta fase merece presupuesto propio: tiene que encontrar UN vertido
        // bueno entre todos los posibles, y con el reparto global se quedaba
        // corta en niveles puerta grandes, que es justo cuando mas la necesita.
        val found = search(start, teapotIndex, capacity, teapotCapacity, maxVisited, isBetter)
            ?: return null
        return Step(found.first, found.second)
    }

    // -----------------------------------------------------------------------
    // BFS clásica
    // -----------------------------------------------------------------------

    private fun solveBySearch(
        start: State,
        requiredOrder: List<Herb>?,
        capacity: Int,
        teapotCapacity: Int,
        maxVisited: Int
    ): List<Pair<Int, Int>>? {
        val isGoal = { s: State ->
            if (requiredOrder != null && requiredOrder.isNotEmpty()) {
                val orderJarCount = s.jars.count { it.herbs() == requiredOrder }
                orderJarCount == 1 && s.jars.indices.all { i ->
                    val stack = s.jars[i]
                    val herbs = stack.herbs()
                    when {
                        i == s.grinder -> herbs.isEmpty()
                        herbs == requiredOrder -> true
                        herbs.isEmpty() -> true
                        else -> herbs.distinct().size == 1 && stack.none { it.frozen }
                    }
                }
            } else {
                s.jars.indices.all { i ->
                    val stack = s.jars[i]
                    val herbs = stack.herbs()
                    when {
                        i == s.grinder -> herbs.isEmpty()
                        herbs.isEmpty() -> true
                        else -> herbs.size == capacity && herbs.distinct().size == 1 &&
                            stack.none { it.hidden || it.frozen }
                    }
                }
            }
        }
        if (isGoal(start)) return emptyList()
        return solvePureJars(start, null, capacity, teapotCapacity, maxVisited, isGoal)
    }

    private fun solvePureJars(
        start: State,
        teapotIndex: Int?,
        capacity: Int,
        teapotCapacity: Int,
        maxVisited: Int,
        isGoal: (State) -> Boolean
    ): List<Pair<Int, Int>>? {
        if (isGoal(start)) return emptyList()
        val found = search(start, teapotIndex, capacity, teapotCapacity, maxVisited, isGoal) ?: return null
        return found.first
    }

    /** BFS genérica: devuelve (movimientos, estado final) o null. */
    private fun search(
        start: State,
        teapotIndex: Int?,
        capacity: Int,
        teapotCapacity: Int,
        maxVisited: Int,
        isGoal: (State) -> Boolean
    ): Pair<List<Pair<Int, Int>>, State>? {
        val queue = ArrayDeque<State>()
        val parent = mutableMapOf<State, State?>()
        val moveMap = mutableMapOf<State, Pair<Int, Int>>()
        val visited = mutableSetOf<State>()

        queue.add(start)
        visited.add(start)
        parent[start] = null

        var solved: State? = null
        while (queue.isNotEmpty() && visited.size < maxVisited) {
            val current = queue.poll() ?: break
            if (isGoal(current)) {
                solved = current
                break
            }
            forEachMove(current, teapotIndex, capacity, teapotCapacity) { from, to, amount ->
                val next = applyPour(current, from, to, amount, teapotIndex, capacity, teapotCapacity)
                if (visited.add(next)) {
                    parent[next] = current
                    moveMap[next] = Pair(from, to)
                    queue.add(next)
                }
            }
        }

        val end = solved ?: return null
        val moves = mutableListOf<Pair<Int, Int>>()
        var cursor: State? = end
        while (cursor != null && parent[cursor] != null) {
            moveMap[cursor]?.let { moves.add(0, it) }
            cursor = parent[cursor]
        }
        return moves to end
    }

    /**
     * Vertidos legales, idénticos a los del motor:
     * - Normal: la hierba superior completa, hasta llenar el destino.
     * - Tetera: cualquier hierba, la ráfaga superior hasta llenar su capacidad,
     *   y nunca se puede usar como origen.
     * - Molinillo: admite una sola hierba de cualquier color y la transforma.
     * - Frascos sellados o helados: no se puede verter de ellos.
     * - Capa pegajosa en la boca: el frasco no suelta nada.
     * - Una capa oculta corta la ráfaga (como en el juego).
     */
    private inline fun forEachMove(
        state: State,
        teapotIndex: Int?,
        capacity: Int,
        teapotCapacity: Int,
        action: (from: Int, to: Int, amount: Int) -> Unit
    ) {
        val stacks = state.jars
        for (fromIdx in stacks.indices) {
            if (teapotIndex != null && fromIdx == teapotIndex) continue
            if (fromIdx in state.sealed) continue
            val srcStack = stacks[fromIdx]
            if (srcStack.isEmpty()) continue
            // El hielo bloquea el frasco entero y la miel bloquea su boca.
            if (srcStack.any { it.frozen }) continue
            val top = srcStack.last()
            if (top.hidden || top.sticky) continue
            val topCount = getTopCount(srcStack)

            for (toIdx in stacks.indices) {
                if (fromIdx == toIdx) continue
                if (toIdx in state.sealed) continue
                val dstStack = stacks[toIdx]

                if (toIdx == state.grinder) {
                    // Una sola capa, de cualquier color; el molinillo la
                    // transforma al recibirla. Con el molinillo lleno no cabe.
                    if (dstStack.isNotEmpty() || topCount <= 0) continue
                    action(fromIdx, toIdx, 1)
                    continue
                }

                val amount = if (teapotIndex != null && toIdx == teapotIndex) {
                    // En la tetera se vierte de una en una (regla del motor).
                    if (dstStack.size >= teapotCapacity) continue
                    1
                } else {
                    if (dstStack.size >= capacity) continue
                    val dstTop = dstStack.lastOrNull()
                    if (dstTop != null && (dstTop.hidden || dstTop.herb != top.herb)) continue
                    minOf(topCount, capacity - dstStack.size)
                }
                if (amount <= 0) continue
                action(fromIdx, toIdx, amount)
            }
        }
    }

    private fun applyPour(
        state: State,
        from: Int,
        to: Int,
        amount: Int,
        teapotIndex: Int?,
        capacity: Int,
        teapotCapacity: Int
    ): State {
        val stacks = state.jars.map { it.toMutableList() }
        repeat(amount) {
            var slot = stacks[from].removeAt(stacks[from].lastIndex)
            // El molinillo muele: guarda la hierba siguiente del ciclo.
            if (to == state.grinder) slot = slot.copy(herb = slot.herb.molida)
            stacks[to].add(slot)
        }
        // El motor revela la capa superior de origen y destino tras cada vertido.
        revealTop(stacks, from)
        revealTop(stacks, to)
        // Y si queda un frasco completo, rompe el hielo de todo el tablero.
        thawIfCompleted(stacks, teapotIndex, state.grinder, capacity)

        val newJars = stacks.map { it.toList() }
        return State(
            newJars,
            unsealAfterMove(newJars, state.sealed, teapotIndex, state.grinder, capacity, teapotCapacity),
            state.grinder
        )
    }

    /** Réplica del calor del motor: un frasco completo descongela todo. */
    private fun thawIfCompleted(
        stacks: List<MutableList<Slot>>,
        teapotIndex: Int?,
        grinder: Int?,
        capacity: Int
    ) {
        val completed = stacks.indices.count { i ->
            if (i == teapotIndex || i == grinder) return@count false
            stacks[i].isNotEmpty() && stacks[i].size == capacity &&
                stacks[i].map { it.herb }.distinct().size == 1
        }
        if (completed == 0) return
        stacks.indices.forEach { i ->
            stacks[i].indices.forEach { j ->
                if (stacks[i][j].frozen) {
                    stacks[i][j] = stacks[i][j].copy(frozen = false)
                }
            }
        }
    }

    private fun revealTop(stacks: List<MutableList<Slot>>, index: Int) {
        val last = stacks[index].lastIndex
        if (last >= 0 && stacks[index][last].hidden) {
            stacks[index][last] = stacks[index][last].copy(hidden = false)
        }
    }

    /** Réplica de GameEngine.processSealedJars para el solver. */
    private fun unsealAfterMove(
        jars: List<List<Slot>>,
        sealed: Set<Int>,
        teapotIndex: Int?,
        grinderIndex: Int?,
        capacity: Int,
        teapotCapacity: Int
    ): Set<Int> {
        if (sealed.isEmpty()) return sealed
        val completed = jars.indices.count { i ->
            if (i == teapotIndex || i == grinderIndex) return@count false
            jars[i].isNotEmpty() && jars[i].size == capacity &&
                jars[i].map { it.herb }.distinct().size == 1
        }
        if (completed == 0) return sealed

        val stillSealed = sealed.toMutableSet()
        var opened = 0
        for (i in jars.indices) {
            if (i in stillSealed && opened < completed) {
                stillSealed.remove(i)
                opened++
            }
        }
        return stillSealed
    }

    /** Ráfaga superior visible: para en una capa oculta, helada o pegajosa. */
    private fun getTopCount(stack: List<Slot>): Int {
        if (stack.isEmpty()) return 0
        // El frasco helado esta bloqueado entero; la miel, si esta en la boca.
        if (stack.any { it.frozen }) return 0
        val top = stack.last()
        if (top.hidden || top.sticky) return 0
        var count = 0
        for (i in stack.indices.reversed()) {
            val slot = stack[i]
            // Igual que Jar.topCount: una capa pegajosa con algo encima hace de
            // tope y el vertido se para ahi.
            if (i != stack.lastIndex && slot.sticky) break
            if (!slot.hidden && slot.herb == top.herb) count++ else break
        }
        return count
    }

    private fun List<Slot>.herbs(): List<Herb> = map { it.herb }
}
