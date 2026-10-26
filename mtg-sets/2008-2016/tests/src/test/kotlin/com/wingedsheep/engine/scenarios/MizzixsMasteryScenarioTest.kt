package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mizzix's Mastery (C15 #29) — {3}{R} Sorcery.
 *
 * "Exile target card that's an instant or sorcery from your graveyard. For each card exiled this
 * way, copy it, and you may cast the copy without paying its mana cost. Exile Mizzix's Mastery.
 * Overload {5}{R}{R}{R}"
 *
 * Pins the two halves of the exile → copy → free-cast chain: the targeted single copy behind a
 * "may", and the overloaded sweep that exiles every instant and sorcery card and offers all the
 * copies — plus that the Mastery exiles itself and leaves non-instant/sorcery cards alone.
 */
class MizzixsMasteryScenarioTest : ScenarioTestBase() {

    init {
        test("cast normally: exiles the target, casts its copy for free, then exiles itself") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mizzix's Mastery")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Shock")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.castSpellTargetingGraveyardCard(1, "Mizzix's Mastery", listOf(bolt)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.isInExile(1, "Lightning Bolt") shouldBe true
            // Only the target was exiled; the other sorcery-or-instant stays put.
            game.isInGraveyard(1, "Shock") shouldBe true
            game.isInExile(1, "Mizzix's Mastery") shouldBe true
            game.isInGraveyard(1, "Mizzix's Mastery") shouldBe false
        }

        test("declining the copy still leaves the card exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mizzix's Mastery")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.castSpellTargetingGraveyardCard(1, "Mizzix's Mastery", listOf(bolt)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInExile(1, "Mizzix's Mastery") shouldBe true
        }

        test("overloaded: every instant and sorcery card is exiled and each copy may be cast") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mizzix's Mastery")
                .withLandsOnBattlefield(1, "Mountain", 8)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Shock")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithOverload(1, "Mizzix's Mastery").error shouldBe null
            game.resolveStack()

            val offer = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            offer.options.map { game.state.getEntity(it)?.get<CardComponent>()?.name } shouldContainExactlyInAnyOrder
                listOf("Lightning Bolt", "Shock")

            var guard = 0
            while (guard++ < 30) {
                when (val pending = game.getPendingDecision()) {
                    is SelectCardsDecision -> game.selectCards(pending.options.take(1))
                    is ChooseTargetsDecision -> game.selectTargets(listOf(game.player2Id))
                    null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                    else -> error("Unexpected decision: $pending")
                }
            }

            // Both copies were cast: 3 + 2 damage.
            game.getLifeTotal(2) shouldBe 15
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInExile(1, "Shock") shouldBe true
            // A creature card isn't an instant or sorcery — it stays in the graveyard.
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Mizzix's Mastery") shouldBe true
        }
    }
}
