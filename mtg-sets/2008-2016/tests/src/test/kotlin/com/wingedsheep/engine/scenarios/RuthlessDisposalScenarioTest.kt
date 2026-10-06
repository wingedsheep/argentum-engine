package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ruthless Disposal — {4}{B} Sorcery. "As an additional cost to cast this spell, discard a card
 * and sacrifice a creature. Two target creatures each get -13/-13 until end of turn."
 */
class RuthlessDisposalScenarioTest : ScenarioTestBase() {
    init {
        test("offers both costs; paying them kills both targets") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Ruthless Disposal")
                .withCardInHand(1, "Swamp")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val info = game.getLegalActions(1).single { it.description == "Cast Ruthless Disposal" }
                .additionalCostInfo.shouldNotBeNull()
            (listOf(info) + info.alsoRequired).map { it.costType }.toSet() shouldBe
                setOf("DiscardCard", "SacrificePermanent")

            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Ruthless Disposal").single(),
                    targets = listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(wurm)),
                    additionalCostPayment = AdditionalCostPayment(
                        discardedCards = game.findCardsInHand(1, "Swamp"),
                        sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!),
                    ),
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Swamp") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true

            game.resolveStack()
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
        }
    }
}
