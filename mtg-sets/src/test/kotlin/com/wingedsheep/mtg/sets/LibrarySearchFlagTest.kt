package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Corpus-wide gate: a printed "search your library" is a real search.
 *
 * Searching a library (CR 701.23) is more than looking at it. A player who can't search libraries
 * (Shadow of Doubt) finds nothing, "whenever a player searches their library" triggers fire, and
 * the rendered ability text reads "Search your library for …" rather than "Look at …". The engine
 * keys all three on the script, not the Oracle text: the library `GatherCardsEffect` must carry
 * `search = true` (it is what the can't-search check and the text renderer read) and the pipeline
 * must end with `EmitLibrarySearchedEventEffect` (what the triggers see).
 * `Patterns.Library.searchLibrary` and friends do both; a hand-spelled
 * `gather(CardSource.FromZone(Zone.LIBRARY, …))` does neither unless asked — the Troop of Ponies
 * bug, where the activation silently ignored Shadow of Doubt and garbled its own text.
 *
 * The sentence match drops reminder text (a keyword's "(… Discard this card: Search your library
 * …)" is the keyword's own, engine-implemented search) and skips phrases where the card doesn't
 * itself search: "can't search", "would search", and a trigger condition ("Whenever an opponent
 * searches their library, …").
 */
class LibrarySearchFlagTest : FunSpec({

    val reminder = Regex("""\([^()]*\)""")
    val sentence = Regex("""\n+|(?<=[.])\s+(?=[A-Z•"“])""")
    val printsSearch = Regex("""\bsearch(?:es)?\b[^.]*?\blibrar(?:y|ies)\b""", RegexOption.IGNORE_CASE)
    val notOwnSearch = Regex(
        """can't search|cannot search|would search|""" +
            """\b(?:whenever|when|if)\b[^,]*\bsearch(?:es|ed)?\b[^,]*\blibrar(?:y|ies)\b[^,]*,""",
        RegexOption.IGNORE_CASE
    )

    fun printedSearches(text: String): List<String> =
        reminder.replace(text, "")
            .split(sentence)
            .map { it.trim() }
            .filter { printsSearch.containsMatchIn(it) && !notOwnSearch.containsMatchIn(it) }

    fun objects(element: JsonElement, into: MutableList<JsonObject>) {
        when (element) {
            is JsonObject -> {
                into += element
                element.values.forEach { objects(it, into) }
            }
            is JsonArray -> element.forEach { objects(it, into) }
            else -> {}
        }
    }

    fun JsonObject.type(): String? = (this["type"] as? JsonPrimitive)?.contentOrNull

    fun JsonObject.readsLibrary(): Boolean {
        val source = this["source"] as? JsonObject ?: return false
        return when (source.type()) {
            "FromZone" -> (source["zone"] as? JsonPrimitive)?.contentOrNull == "Library"
            "FromMultipleZones" -> (source["zones"] as? JsonArray).orEmpty()
                .any { (it as? JsonPrimitive)?.contentOrNull == "Library" }
            else -> false
        }
    }

    // Player.You is the default for both the source's player and the gather's searcher, and a
    // default is omitted from the JSON.
    fun JsonElement?.isYou(): Boolean = when (this) {
        null -> true
        is JsonPrimitive -> contentOrNull == "You"
        is JsonObject -> type() == "You"
        else -> false
    }

    // The searcher searches their own library: "search your library" (both default to Player.You),
    // or another player searching theirs ("its controller may search their library" — Sandworm,
    // whose gather names the library's player as its searcher). Searching *another* player's
    // library (Cranial Extraction: you search theirs) is not "a player searches their library",
    // the event EmitLibrarySearchedEventEffect reports.
    fun JsonObject.readsOwnLibrary(): Boolean {
        val player = (this["source"] as? JsonObject)?.get("player")
        val searcher = this["searcher"]
        return if (player.isYou()) searcher.isYou() else player == searcher
    }

    // A search of another player's library that the controller performs.
    fun JsonObject.controllerSearchesOthersLibrary(): Boolean =
        !(this["source"] as? JsonObject)?.get("player").isYou() && this["searcher"].isYou()

    // "Its controller may search their library", "that player … searches their library": someone
    // other than the controller searches their own library.
    val othersOwnSearch = Regex("""\bsearch(?:es)?\s+their\s+library""", RegexOption.IGNORE_CASE)

    data class Script(
        val searchGathers: Int,
        val ownSearchGathers: Int,
        val controllerSearchesOthers: Int,
        val plainLibraryGathers: Int,
        val emitsSearched: Boolean,
        // The card's own text plus every face's (an Omen or Adventure half carries its own).
        val oracleTexts: List<String>
    )

    fun script(card: CardDefinition): Script {
        val all = mutableListOf<JsonObject>()
        objects(CardSerialization.json.encodeToJsonElement(CardDefinition.serializer(), card), all)
        val libraryGathers = all.filter { it.type() == "GatherCards" && it.readsLibrary() }
        val searching = libraryGathers.filter { (it["search"] as? JsonPrimitive)?.booleanOrNull == true }
        return Script(
            searchGathers = searching.size,
            ownSearchGathers = searching.count { it.readsOwnLibrary() },
            controllerSearchesOthers = searching.count { it.controllerSearchesOthersLibrary() },
            plainLibraryGathers = libraryGathers.size - searching.size,
            emitsSearched = all.any { it.type() == "EmitLibrarySearchedEvent" },
            oracleTexts = (listOf(card.oracleText.orEmpty()) +
                all.mapNotNull { (it["oracleText"] as? JsonPrimitive)?.contentOrNull }).distinct()
        )
    }

    // Cards exempted from the gate, each with the reason. Keep it short; a new entry needs a real
    // one, and an entry that would pass without it fails the stale check below.
    val allowlist = mapOf(
        // "Search its controller's graveyard, hand, and library": *you* search *another* player's
        // library. The search runs inside a per-player loop where `Player.You` is that other player,
        // so it is flagged (Shadow of Doubt applies) but deliberately emits no searched event — that
        // event means "a player searched their own library", which isn't what happened.
        "Test of Talents" to "searches another player's library from a per-player loop",
        "The End" to "searches another player's library from a per-player loop",
        "Deadly Cover-Up" to "searches another player's library from a per-player loop",
    )

    val cards = MtgSetCatalog.all.flatMap { set -> set.cards.map { set.code to it } }
        .distinctBy { (_, card) -> card.name }

    fun offence(code: String, card: CardDefinition): String? {
        val s = script(card)
        val printed = s.oracleTexts.flatMap { printedSearches(it) }
        if (printed.isEmpty()) return null
        return when {
            s.searchGathers == 0 ->
                "[$code] ${card.name}: prints a library search but no library gather sets search = true " +
                    "(${s.plainLibraryGathers} plain library gather(s)) — \"${printed.first()}\""
            s.ownSearchGathers > 0 && !s.emitsSearched ->
                "[$code] ${card.name}: searches your library but never emits EmitLibrarySearchedEventEffect"
            s.ownSearchGathers == 0 && s.controllerSearchesOthers > 0 &&
                printed.any { othersOwnSearch.containsMatchIn(it) } ->
                "[$code] ${card.name}: another player searches their own library, but the gather's " +
                    "searcher is the controller (set GatherCardsEffect.searcher to that player)"
            else -> null
        }
    }

    val printing = cards.count { (_, card) -> script(card).oracleTexts.any { printedSearches(it).isNotEmpty() } }
    val offenders = cards
        .filter { (_, card) -> card.name !in allowlist }
        .mapNotNull { (code, card) -> offence(code, card) }

    test("a card whose Oracle text searches a library flags the search and emits the searched event") {
        if (offenders.isNotEmpty()) {
            println("=== library searches missing the search flag or searched event: ${offenders.size} ===")
            offenders.forEach { println("  $it") }
        }
        offenders.shouldBeEmpty()
    }

    // Independent of Oracle wording: a flagged search of your own library that never emits the
    // event is a search no "whenever a player searches their library" trigger can see.
    test("every flagged library search emits the searched event") {
        val silent = cards.filter { (_, card) -> card.name !in allowlist }.mapNotNull { (code, card) ->
            val s = script(card)
            if (s.ownSearchGathers > 0 && !s.emitsSearched) "[$code] ${card.name}" else null
        }
        silent.shouldBeEmpty()
    }

    test("every allowlisted card still fails the gate it is exempted from") {
        val stale = allowlist.keys.filter { name ->
            val (code, card) = cards.firstOrNull { it.second.name == name } ?: return@filter true
            offence(code, card) == null
        }
        stale.shouldBeEmpty()
    }

    // The gate can only fail on sentences it recognises. A floor keeps a regex tweak from turning it
    // green by matching nothing.
    test("the gate covers the cards it is supposed to cover") {
        println("cards printing a library search: $printing")
        check(printing >= 200) { "only $printing cards matched; the search regex has stopped working" }
    }
})
