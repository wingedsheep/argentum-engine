package com.wingedsheep.assay.explore

import com.wingedsheep.assay.compile.CardCompiler
import com.wingedsheep.assay.compile.CompileResult
import com.wingedsheep.assay.corpus.SetMembership
import com.wingedsheep.assay.gate.DeclineKey
import com.wingedsheep.assay.gate.Differential
import com.wingedsheep.assay.gate.DifferentialReport
import com.wingedsheep.assay.gate.Touchstone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.charset.StandardCharsets

/**
 * The explorer, minus a transport: the sweep, the caches, and one handler per route.
 *
 * ## Why this is separate from [ExploreServer]
 *
 * The page has two servers now. `assay explore` binds its own loopback [ExploreServer] and mounts
 * the routes at `/api/`; `game-server` mounts the identical handlers under `/api/assay/explorer/`
 * so the Set Completion view can frame the live tool beside the coverage grid. Those are two
 * transports, and the thing that must **not** be duplicated between them is which routes exist and
 * what each one computes — a second copy would be a second explorer, drifting from this one exactly
 * as fast as anyone edited either.
 *
 * So the split is by transport, not by feature: everything a route *decides* is here, and each
 * server is reduced to moving bytes. The module's standing rule that the explorer is "a view, never
 * a second source of truth" is the same argument one level down — every number below still comes out
 * of `FinenessReport`, [Touchstone] or [Differential], and nothing here computes one of its own.
 *
 * ## Cost model
 *
 * Unchanged from when this lived in the server. Two things are expensive and both are done once. The
 * corpus **sweep** ([AssayIndex]) runs on a background thread started by [startSweep], so a page can
 * open immediately and show its progress; the **differential** runs on first request to its own
 * route and is then cached, because it decodes thousands of goldens and most sessions never ask for
 * it. Everything else — a card, a custom parse, the rule tree — is per-request in milliseconds.
 *
 * A consequence worth stating for the embedded case: [startSweep] is deliberately *not* called from
 * the constructor. `game-server` builds this lazily on the first request to the explorer, so a
 * production boot that never serves the tool never downloads a corpus.
 *
 * The dependency rule holds — there is nothing here but the JDK and this module's own gates. The
 * one thing a transport may add is an [AiReadingProvider]: `game-server` has the engine AI on its
 * classpath and passes one, so the card page can show what the AI understands of a card; `assay
 * explore` has no AI and passes none, and the `ai` route says so rather than failing.
 */
class ExploreApi(
    private val refresh: Boolean = false,
    private val aiReading: AiReadingProvider? = null,
) {

    private val touchstone = Touchstone()

    @Volatile private var index: AssayIndex? = null
    @Volatile private var goldens: GoldenIndex = GoldenIndex.load()
    @Volatile private var swept = 0
    @Volatile private var sweptFraction = 0.0
    @Volatile private var failure: String? = null
    @Volatile private var sweepStarted = false

    /** Guarded by [differentialLock]; computed on first request because it is the expensive one. */
    private var differential: DifferentialReport? = null
    private val differentialLock = Any()

    /**
     * Kick off the corpus sweep on a daemon thread. Idempotent, so a transport may call it on every
     * request without having to remember whether it already has.
     */
    @Synchronized
    fun startSweep() {
        if (sweepStarted) return
        sweepStarted = true
        Thread({ sweep() }, "assay-explore-sweep").apply { isDaemon = true }.start()
    }

    private fun sweep() {
        try {
            index = AssayIndex.build(refresh = refresh) { cards, fraction ->
                swept = cards
                sweptFraction = fraction
            }
        } catch (e: Exception) {
            // A sweep that cannot start — no cached bulk file and no network — must leave the page
            // usable rather than a dead socket: the live parser and the rule tree need no corpus.
            failure = e.message ?: e::class.simpleName ?: "the corpus sweep failed"
        }
    }

    // -----------------------------------------------------------------------------------------
    // The page
    // -----------------------------------------------------------------------------------------

    /**
     * The explorer page, with its request prefix substituted in.
     *
     * @param apiBase what the page prepends to a route name. Must end in `/` — the page concatenates
     *   rather than joins, so a missing slash produces `…explorerstatus` and every request 404s.
     */
    fun page(apiBase: String = DEFAULT_API_BASE): ByteArray {
        require(apiBase.endsWith("/")) { "apiBase must end in '/', was \"$apiBase\"" }
        val html = javaClass.getResourceAsStream(PAGE)?.readBytes()
            ?: error("$PAGE is missing from the jar — the explorer page is a resource of :oracle-assay")
        return String(html, StandardCharsets.UTF_8)
            .replace(API_BASE_TOKEN, apiBase)
            .toByteArray(StandardCharsets.UTF_8)
    }

    // -----------------------------------------------------------------------------------------
    // Routes
    // -----------------------------------------------------------------------------------------

    /**
     * Answer a GET route. [param] reads a query parameter by name, already URL-decoded.
     *
     * An unknown route is an error *payload* rather than an exception, so a transport never has to
     * decide what a 404 looks like and the page shows the same message from either server.
     */
    fun get(route: String, param: (String) -> String? = { null }): JsonElement = when (route) {
        "status" -> status()
        "overview" -> ready { Views.overview(it, goldens) }
        "search" -> ready { Views.search(it, param("q").orEmpty()) }
        "cards" -> cards(param)
        "card" -> card(param)
        "ai" -> ai(param)
        "declines" -> declines(param)
        "decline" -> decline(param)
        "grammar" -> Views.grammar(index)
        "differential" -> differential()
        else -> errorPayload("no such route \"$route\"")
    }

    /** Answer a POST route. [body] is the raw request body; both routes here take a JSON object. */
    fun post(route: String, body: String?): JsonElement {
        val fields = body?.let { runCatching { json.parseToJsonElement(it) as JsonObject }.getOrNull() }
            ?: return errorPayload("expected a JSON object")
        return when (route) {
            "parse" -> parse(fields)
            "probe" -> probe(fields)
            else -> errorPayload("no such route \"$route\"")
        }
    }

    /** The GET routes, for a transport that has to register them one at a time. */
    val getRoutes: Set<String> get() = GET_ROUTES

    /** The POST routes. */
    val postRoutes: Set<String> get() = POST_ROUTES

    // -----------------------------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------------------------

    private fun status(): JsonObject = JsonObject(
        mapOf(
            "ready" to JsonPrimitive(index != null),
            "swept" to JsonPrimitive(index?.report?.cards ?: swept),
            "progress" to JsonPrimitive(if (index != null) 1.0 else sweptFraction),
            "goldens" to JsonPrimitive(goldens.size),
            "error" to JsonPrimitive(failure ?: ""),
            "aiReading" to JsonPrimitive(aiReading != null),
        )
    )

    /** Every corpus-backed route answers "still sweeping" rather than blocking a request thread. */
    private fun ready(body: (AssayIndex) -> JsonElement): JsonElement {
        val current = index ?: return JsonObject(
            mapOf(
                "indexing" to JsonPrimitive(true),
                "swept" to JsonPrimitive(swept),
                "progress" to JsonPrimitive(sweptFraction),
                "error" to JsonPrimitive(failure ?: ""),
            )
        )
        return body(current)
    }

    private fun cards(param: (String) -> String?): JsonElement = ready { index ->
        val set = param("set")?.takeIf { it.isNotBlank() }
        val filter = CardFilter(
            state = param("state"),
            set = set,
            // Resolved here rather than during the sweep, and on the request thread. A set list is
            // ~200 KB and then memoized for the process, so exactly one request per set pays for it;
            // pre-loading every set at startup would download 1,047 lists to serve a filter most
            // sessions never touch, and baking membership into the sweep would make the corpus
            // depend on the network for a question only this filter asks.
            setCards = set?.let { SetMembership.of(it) },
            query = param("q")?.takeIf { it.isNotBlank() },
            scopeOnly = param("scope") == "1",
            goldenOnly = param("golden") == "1",
            goldens = index.goldenNames,
            // "AI gaps": implemented cards the engine AI does not read fully. Only meaningful with a
            // provider, and only complete once its corpus index is; the payload says which.
            aiGaps = param("ai") == "gaps" && aiReading != null,
            aiReadsFully = { name -> aiReading?.readsFully(name) },
        )
        Views.cards(
            index = index,
            filter = filter,
            offset = param("offset")?.toIntOrNull() ?: 0,
            limit = (param("limit")?.toIntOrNull() ?: 100).coerceIn(1, 500),
            ai = aiReading?.let { AiColumn(available = true, ready = it.corpusReady()) } ?: AiColumn(available = false, ready = false),
        )
    }

    private fun card(param: (String) -> String?): JsonElement = ready { index ->
        val name = param("name").orEmpty()
        Views.cardPage(index, goldens, touchstone, name)
            ?: errorPayload("no card named \"$name\" in the Oracle bulk")
    }

    /**
     * What the engine AI reads of one card — its own route, not a field of `card`, so the card page
     * paints Assay's half at once and this panel fills in (or fails) on its own.
     *
     * Which definition is read, in order: the engine's own (the live registry, through the
     * provider); the hand-written golden on disk; and for a card nobody has implemented, Assay's
     * whole-card compile — the same fail-closed [CardCompiler] the Scenario Builder sandbox plays,
     * labelled `preview` so the page can say it is what the AI *would* read if the card were written
     * the way Assay reads it. A card Assay cannot compile has nothing to read, and says why.
     */
    private fun ai(param: (String) -> String?): JsonElement {
        val provider = aiReading ?: return JsonObject(mapOf("available" to JsonPrimitive(false)))
        return ready { index ->
            val name = param("name").orEmpty()
            val card = index.card(name) ?: return@ready errorPayload("no card named \"$name\" in the Oracle bulk")
            val implemented = provider.implemented(card.name)
                ?: provider.implemented(card.name.substringBefore(" // "))
                ?: goldens.card(card.name)?.definition
            val (source, definition) = if (implemented != null) {
                "implemented" to implemented
            } else {
                when (val compiled = CardCompiler.compile(card, touchstone)) {
                    is CompileResult.Compiled -> "preview" to compiled.definition
                    is CompileResult.Declined -> return@ready buildJsonObject {
                        put("available", true)
                        put("source", "none")
                        put(
                            "why",
                            compiled.declines.firstOrNull()?.let { d ->
                                d.detail + (d.line?.let { " — \"$it\"" } ?: "")
                            } ?: "Assay does not read this card whole",
                        )
                    }
                }
            }
            val view = provider.read(definition)
            buildJsonObject {
                put("available", true)
                put("source", source)
                put("reading", json.encodeToJsonElement(AiReadingView.serializer(), view))
            }
        }
    }

    private fun declines(param: (String) -> String?): JsonElement = ready { index ->
        Views.declines(
            index = index,
            ranking = ranking(param),
            query = param("q"),
            limit = param("limit")?.toIntOrNull() ?: 100,
        )
    }

    private fun decline(param: (String) -> String?): JsonElement = ready { index ->
        val key = param("key").orEmpty()
        Views.decline(index, ranking(param), key) ?: errorPayload("no decline family \"$key\"")
    }

    /**
     * The feasibility probe. A POST because the span and its replacement are free text — a regex of
     * Oracle punctuation in a query string is a URL-encoding accident waiting to happen — and
     * because it is the one route here that runs a measurement rather than reading one.
     */
    private fun probe(fields: JsonObject): JsonElement = ready { index ->
        Views.probe(
            index = index,
            touchstone = touchstone,
            ranking = DeclineKey.byName(fields.str("by")) ?: DeclineKey.TAIL,
            key = fields.str("key"),
            find = fields.str("find"),
            replace = fields.str("replace"),
            regex = fields.str("regex") == "true",
        )
    }

    private fun parse(fields: JsonObject): JsonElement = Views.parsed(
        touchstone,
        ParseRequest(
            name = fields.str("name"),
            manaCost = fields.str("manaCost"),
            typeLine = fields.str("typeLine"),
            oracleText = fields.str("oracleText"),
        ),
    )

    private fun differential(): JsonElement = ready {
        if (!goldens.available) {
            return@ready errorPayload(
                "no hand-written card goldens found — run `just test-class CardDefinitionSnapshotTest`"
            )
        }
        val report = synchronized(differentialLock) {
            differential ?: Differential(touchstone).run().also { differential = it }
        }
        Views.differential(report)
    }

    // -----------------------------------------------------------------------------------------
    // Plumbing
    // -----------------------------------------------------------------------------------------

    private fun ranking(param: (String) -> String?): DeclineKey =
        DeclineKey.byName(param("by")) ?: DeclineKey.TOKEN

    private fun JsonObject.str(key: String): String = (this[key] as? JsonPrimitive)?.content.orEmpty()

    private fun errorPayload(message: String): JsonObject =
        JsonObject(mapOf("error" to JsonPrimitive(message)))

    /**
     * Render a payload, turning a handler that threw into a readable error in the page rather than
     * an empty response the browser reports as a network failure.
     */
    fun encode(body: () -> JsonElement): String =
        runCatching { json.encodeToString(JsonElement.serializer(), body()) }
            .getOrElse { e ->
                """{"error":${JsonPrimitive(e.message ?: e::class.simpleName ?: "internal error")}}"""
            }

    companion object {
        /** Where `assay explore`'s own server mounts the routes. */
        const val DEFAULT_API_BASE = "/api/"

        private const val PAGE = "/explorer/index.html"
        private const val API_BASE_TOKEN = "%%ASSAY_API_BASE%%"

        private val json = Json { prettyPrint = false }

        private val GET_ROUTES = setOf(
            "status", "overview", "search", "cards", "card", "ai", "declines", "decline", "grammar",
            "differential",
        )
        private val POST_ROUTES = setOf("parse", "probe")
    }
}
