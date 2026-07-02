package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.name
import kotlin.io.path.readText

class MtgSetCatalogTest : FunSpec({

    // Guards auto-discovery: every `*Set.kt` on disk must surface in MtgSetCatalog.all (built by
    // CardDiscovery.findSets). A set declared as something other than an `object`, or otherwise
    // missed by the classpath scan, would silently vanish from the catalog — this catches it.
    test("every <Name>Set.kt under definitions is discovered into MtgSetCatalog.all") {
        val definitionsDir = Paths.get(
            "src/main/kotlin/com/wingedsheep/mtg/sets/definitions"
        ).toAbsolutePath()

        val setFilesInTree = Files.walk(definitionsDir).use { stream ->
            stream
                .filter { it.name.endsWith("Set.kt") }
                .filter { it.readText().contains(": MtgSet") }
                .map { it.name.removeSuffix(".kt") }
                .toList()
                .sorted()
        }

        val registeredNames = MtgSetCatalog.all
            .map { it::class.simpleName!! }
            .sorted()

        setFilesInTree.shouldContainExactlyInAnyOrder(registeredNames)
    }

    test("set codes are unique") {
        val duplicates = MtgSetCatalog.all
            .groupBy { it.code }
            .filterValues { it.size > 1 }
            .keys
        duplicates.shouldBeEmpty()
    }

    // The same hollow-set failure GameBeansConfig guards at server boot, caught here in CI: a set
    // that contributes no cards, printings, or basic lands has almost always typo'd its
    // CARDS_PACKAGE. (All-reprint sets like Eighth Edition have no own cards but do carry printings.)
    test("no registered set is hollow") {
        assertSoftly {
            for (set in MtgSetCatalog.all) {
                withClue("${set.code} (${set.displayName})") {
                    val hasContent = set.cards.isNotEmpty() ||
                        set.printings.isNotEmpty() ||
                        set.basicLands.isNotEmpty()
                    hasContent shouldBe true
                }
            }
        }
    }

    // The orphaned-reprint trap: a set declares `Printing(...)` rows in its package (e.g. a reprint
    // of a card whose canonical CardDefinition lives in an earlier set) but forgets to wire
    // `override val printings by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }`. The rows
    // then exist on disk but never reach MtgSet.printings — so PrintingRegistry, the deckbuilder's
    // printing picker, sealed/draft, and the Set Completion coverage view all silently miss them.
    // This caught ONS/LGN/MBS/ONE. Scanning each set's own package recursively (acceptPackages is
    // recursive) finds every Printing in its subtree without assuming the `.cards` convention.
    test("every Printing row in a set's package is exposed via MtgSet.printings") {
        assertSoftly {
            for (set in MtgSetCatalog.all) {
                val declared = CardDiscovery.findPrintingsIn(set::class.java.packageName)
                if (declared.isEmpty()) continue
                val exposed = set.printings.toSet()
                val orphaned = declared.filterNot { it in exposed }.map { it.name }.sorted()
                withClue(
                    "${set.code} (${set.displayName}) has Printing row(s) not wired into " +
                        "MtgSet.printings: $orphaned — add " +
                        "`override val printings by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }`"
                ) {
                    orphaned.shouldBeEmpty()
                }
            }
        }
    }

    // The duplicate-canonical trap (the Giant Growth bug): a card's full `card(...)` CardDefinition
    // is the *canonical* the rules engine keys on, and CardRegistry stores it by name with
    // last-write-wins. Sets register chronologically, so a second canonical for the same name in a
    // later set silently overwrites the earlier one — the drafted early-print card then resolves to
    // the later set's art/oracle. Only a card's *earliest* real printing may carry a canonical
    // `card(...)`; every later set must contribute a `Printing(...)` row instead. Basic lands are the
    // one legitimate multi-set canonical (each set defines its own art variants) and are excluded.
    test("no card name has a canonical CardDefinition in more than one set") {
        val canonicalsByName = MtgSetCatalog.all
            .flatMap { set -> set.cards.map { set.code to it } }
            .filterNot { (_, card) -> card.typeLine.isBasicLand }
            .groupBy({ (_, card) -> card.name }, { (code, _) -> code })

        val duplicates = canonicalsByName
            .filterValues { it.distinct().size > 1 }
            .toSortedMap()

        withClue(
            "Card name(s) with a canonical CardDefinition in >1 set — only the earliest printing " +
                "may carry a `card(...)`; later sets must use a `Printing(...)` row (CLAUDE.md " +
                "\"Reprints\"):\n" +
                duplicates.entries.joinToString("\n") { (name, codes) ->
                    "  $name -> ${codes.distinct().sorted()}"
                }
        ) {
            duplicates.keys.shouldBeEmpty()
        }
    }

    test("release dates, when present, are parseable ISO YYYY-MM-DD") {
        assertSoftly {
            for (set in MtgSetCatalog.all) {
                val date = set.releaseDate ?: continue
                withClue("${set.code} releaseDate=$date") {
                    val parsed = runCatching {
                        LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
                    }.isSuccess
                    parsed shouldBe true
                }
            }
        }
    }
})
