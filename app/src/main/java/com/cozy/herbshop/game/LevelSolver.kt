package com.cozy.herbshop.game

import com.cozy.herbshop.data.model.Herb
import com.cozy.herbshop.data.model.Jar
import com.cozy.herbshop.data.model.JarLayer
import java.util.ArrayDeque

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
    data class Slot(val herb: Herb, val hidden: Boolean = false, val sticky: Boolean = false)

    data class State(val jars: List<List<Slot>>, val sealed: Set<Int>)

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
    private companion object {
        const val DEFAULT_MAX_VISITED = 60_000

        /** Tope de vertidos del reparto sin busqueda, para que no se ciclie. */
        const val MAX_PASOS_REPARTO = 600
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
        val capacity = initialJars.filterNot { it.isTeapot }.maxOfOrNull { it.capacity } ?: 4
        val teapotCapacity = teapotIndex?.let { initialJars[it].capacity } ?: capacity

        // Las capas con su estado: no hace falta saber cuales estan escondidas, porque
        // el motor destapa la cima de origen y destino tras cada vertido. Si se
        // guarda la marca de pegajosa, que el reparto tiene que respetar igual que
        // el motor.
        val frascos: MutableList<MutableList<JarLayer>> =
            initialJars.map { jar -> jar.layers.toMutableList() }.toMutableList()
        // Un frasco sellado no se puede tocar hasta que se abren otros completando
        // frascos, igual que hace el motor: se van abriendo sobre la marcha.
        val sellados = initialJars.map { it.isSealed }.toMutableList()
        val movimientos = mutableListOf<Pair<Int, Int>>()

        fun limiteDe(indice: Int): Int = if (indice == teapotIndex) teapotCapacity else capacity

        // Igual que el motor: de un frasco con la cima escondida no se puede
        // verter nada. Si el reparto no lo respetara, su solucion leeria jugadas
        // que luego el motor rechaza.
        fun jugables(): List<Int> =
            frascos.indices.filter {
                it != teapotIndex && !sellados[it] && frascos[it].lastOrNull()?.isHidden != true
            }

        fun bloqueSuperior(indice: Int): Int {
            val frasco = frascos[indice]
            if (frasco.isEmpty()) return 0
            if (frasco.last().isHidden) return 0
            val cima = frasco.last().herb
            var n = 0
            for (k in frasco.indices.reversed()) {
                // La capa pegajosa hace de tope, igual que en el motor.
                if (k != frasco.lastIndex && frasco[k].isSticky) break
                if (frasco[k].herb == cima) n++ else break
            }
            return n
        }

        // Meta del motor: frasco vacio, o lleno y de una sola hierba.
        fun terminado(indice: Int): Boolean {
            if (indice == teapotIndex) {
                return recipe == null || frascos[indice].map { it.herb } == recipe
            }
            val frasco = frascos[indice]
            if (frasco.isEmpty()) return true
            if (frasco.map { it.herb }.distinct().size > 1) return false
            // Sin receta hay que llenar el frasco entero: sobran cuatro capas de
            // cada hierba. Con receta no, porque la tetera se lleva parte de
            // ellas y lo que queda nunca llegaria a cuatro. Es la misma regla que
            // aplica el motor.
            return recipe != null || frasco.size == limiteDe(indice)
        }

        fun completo(indice: Int): Boolean {
            if (indice == teapotIndex) return false
            val frasco = frascos[indice]
            return frasco.size == limiteDe(indice) && frasco.map { it.herb }.distinct().size == 1
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

        fun verter(desde: Int, hacia: Int, cuanto: Int) {
            repeat(cuanto) {
                frascos[hacia].add(frascos[desde].removeAt(frascos[desde].lastIndex))
            }
            movimientos.add(desde to hacia)
            abrirSellados()
        }

        var pasos = 0
        while (pasos++ < MAX_PASOS_REPARTO) {
            // Los frascos de hierba van primero: vacios, o de una sola hierba.
            val otrosListos = frascos.indices.all { it == teapotIndex || terminado(it) }
            // Meta del motor: frasco vacio, o lleno y de una sola hierba; y la
            // tetera con la receta exacta.
            val teteraLlena = teapotIndex == null || recipe == null ||
                frascos[teapotIndex].map { it.herb } == recipe
            if (otrosListos && teteraLlena) return movimientos

            val libres = jugables()
            var hecho = false

            if (otrosListos) {
                // Con los frascos ya puros se sirve la tetera: cada hierba esta
                // arriba en algun frasco, asi que sale siempre. Servirla antes de
                // tiempo hacia gastar capas que hacian falta para ordenar y el
                // reparto se quedaba atascado.
                val tetera = frascos[teapotIndex!!]
                val faltan = recipe ?: return null
                // Si la tetera ya esta llena y no es la receta, no hay receta que
                // servir: no se puede pedir la hierba que le sigue, no existe hueco.
                // Ojo con el tipo: [tetera] es una lista de JarLayer y [faltan] una de
                // Herb. Compararlas tal cual nunca da igual (una capa no es una
                // hierba) y el reparto daba cualquier nivel con receta por
                // imposible, que hacia caer al generador en el nivel de reserva.
                if (tetera.size >= faltan.size || tetera.map { it.herb } != faltan.take(tetera.size)) {
                    return null
                }
                val falta = faltan[tetera.size]
                val origen = libres.firstOrNull { frascos[it].lastOrNull()?.herb == falta }
                if (origen != null) {
                    verter(origen, teapotIndex, 1)
                    hecho = true
                }
            } else {
                // Ordenar: bloque superior a un frasco del mismo color con sitio,
                // y si no hay sitio a un frasco vacio.
                for (i in libres) {
                    val cuanto = bloqueSuperior(i)
                    if (cuanto == 0) continue
                    val cima = frascos[i].last().herb
                    val destino = libres.firstOrNull { j ->
                        j != i && frascos[j].isNotEmpty() && frascos[j].last().herb == cima &&
                            frascos[j].size < limiteDe(j)
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
                    for (i in libres) {
                        val cuanto = bloqueSuperior(i)
                        if (cuanto == 0) continue
                        val destino = libres.firstOrNull { j -> j != i && frascos[j].isEmpty() }
                        if (destino != null) {
                            verter(i, destino, cuanto)
                            hecho = true
                            break
                        }
                    }
                }
            }
            if (!hecho) return null
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
        val capacity = initialJars.filterNot { it.isTeapot }.maxOfOrNull { it.capacity }
            ?: initialJars.maxOfOrNull { it.capacity }
            ?: 4
        val teapotCapacity = teapotIndex?.let { initialJars[it].capacity } ?: capacity
        val start = State(
            jars = initialJars.map { jar -> jar.layers.map { Slot(it.herb, it.isHidden, it.isSticky) } },
            sealed = initialJars.indices.filter { initialJars[it].isSealed }.toSet()
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
                i == teapotIndex || s.jars[i].isEmpty() || s.jars[i].herbs().distinct().size == 1
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
                orderJarCount == 1 && s.jars.all { stack ->
                    val herbs = stack.herbs()
                    herbs == requiredOrder || herbs.isEmpty() || herbs.distinct().size == 1
                }
            } else {
                s.jars.all { stack ->
                    val herbs = stack.herbs()
                    herbs.isEmpty() || (herbs.size == capacity && herbs.distinct().size == 1)
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
     * - Sellado: no se puede usar ni como origen ni como destino.
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
            val top = srcStack.last()
            if (top.hidden) continue
            val topCount = getTopCount(srcStack)

            for (toIdx in stacks.indices) {
                if (fromIdx == toIdx) continue
                if (toIdx in state.sealed) continue
                val dstStack = stacks[toIdx]

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
            val slot = stacks[from].removeAt(stacks[from].lastIndex)
            stacks[to].add(slot)
        }
        // El motor revela la capa superior de origen y destino tras cada vertido.
        revealTop(stacks, from)
        revealTop(stacks, to)

        val newJars = stacks.map { it.toList() }
        return State(newJars, unsealAfterMove(newJars, state.sealed, teapotIndex, capacity, teapotCapacity))
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
        capacity: Int,
        teapotCapacity: Int
    ): Set<Int> {
        if (sealed.isEmpty()) return sealed
        val completed = jars.indices.count { i ->
            if (i == teapotIndex) return@count false
            val jarCapacity = if (i == teapotIndex) teapotCapacity else capacity
            jars[i].isNotEmpty() && jars[i].size == jarCapacity &&
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

    /** Ráfaga superior visible: para o se corta en una capa oculta. */
    private fun getTopCount(stack: List<Slot>): Int {
        if (stack.isEmpty()) return 0
        val top = stack.last()
        if (top.hidden) return 0
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
