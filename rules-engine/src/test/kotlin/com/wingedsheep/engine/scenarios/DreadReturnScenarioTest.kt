package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dread Return (TSP #104) — reanimation for {2}{B}{B}, or for free from the graveyard by
 * sacrificing three creatures (a flashback cost with no mana component at all).
 */
class DreadReturnScenarioTest : ScenarioTestBase() {
    init {
        test("returns a creature card from your graveyard to the battlefield") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Dread Return")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingGraveyardCard(1, "Dread Return", 1, "Grizzly Bears")
                .error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("flashback sacrifices three creatures and pays no mana") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInGraveyard(1, "Dread Return")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(1, "Devoted Hero")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            val dreadReturn = game.findCardsInGraveyard(1, "Dread Return").single()
            val fodder = listOf("Grizzly Bears", "Glory Seeker", "Devoted Hero")
                .map { game.findPermanent(it)!! }
            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = dreadReturn,
                    targets = listOf(ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)),
                    additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = fodder),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                ),
            )
            withClue("no lands in play — the flashback cost is purely the sacrifice: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()
            while (game.hasPendingDecision()) {
                val d = game.state.pendingDecision as? SelectCardsDecision ?: break
                game.selectCards(d.options.take(d.minSelections))
                if (game.state.stack.isNotEmpty()) game.resolveStack()
            }

            withClue("three creatures sacrificed to pay flashback") {
                game.findPermanents("Grizzly Bears").size +
                    game.findPermanents("Glory Seeker").size +
                    game.findPermanents("Devoted Hero").size shouldBe 0
            }
            withClue("Hill Giant is reanimated and the spell exiles itself") {
                game.findPermanent("Hill Giant") shouldNotBe null
                game.isInExile(1, "Dread Return") shouldBe true
            }
        }
    }
}
