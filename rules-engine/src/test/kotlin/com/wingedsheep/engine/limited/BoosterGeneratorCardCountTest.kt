package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Pins [BoosterGenerator.SetConfig.distinctCardCount], the "X cards" number shown in set pickers.
 *
 * Commander / precon sets carry most of their cards as reprint [Printing] rows whose canonical
 * [CardDefinition] lives in an earlier set — those reprints never appear in [SetConfig.cards], so a
 * naive `cards.size` undercounts the set (Bloomburrow Commander showed 50 instead of ~109).
 */
class BoosterGeneratorCardCountTest : DescribeSpec({

    fun card(name: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = "1", imageUri = "canonical/$name.jpg"),
    ).copy(setCode = "BLC")

    fun reprint(name: String, frame: Boolean = false) = Printing(
        oracleId = name,
        name = name,
        setCode = "BLC",
        collectorNumber = "200",
        imageUri = "reprint/$name.jpg",
        frameEffects = if (frame) listOf("showcase") else emptyList(),
        borderColor = if (frame) "borderless" else null,
    )

    fun config(cards: List<CardDefinition>, printings: List<Printing>) =
        BoosterGenerator.SetConfig(
            setCode = "BLC",
            setName = "Bloomburrow Commander",
            cards = cards,
            basicLands = emptyList(),
            printings = printings,
        )

    describe("distinctCardCount") {

        it("counts only full card definitions when there are no printings") {
            config(listOf(card("Abrade"), card("Beast Within")), emptyList())
                .distinctCardCount shouldBe 2
        }

        it("counts reprint printings whose canonical lives in another set") {
            // 2 new cards + 3 reprints (canonicals elsewhere) = 5 distinct cards.
            config(
                cards = listOf(card("Abrade"), card("Beast Within")),
                printings = listOf(reprint("Adarkar Wastes"), reprint("Barren Moor"), reprint("Command Tower")),
            ).distinctCardCount shouldBe 5
        }

        it("does not double-count an alternate-frame printing of a card defined in the set") {
            // "Abrade" appears as both a full definition and a showcase printing — still one card.
            config(
                cards = listOf(card("Abrade")),
                printings = listOf(reprint("Abrade", frame = true), reprint("Command Tower")),
            ).distinctCardCount shouldBe 2
        }
    }
})
