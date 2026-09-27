package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Oni Possession (CHK #135) — +3/+3, trample, becomes a Demon Spirit (losing other creature
 * types), and its controller sacrifices a creature at the beginning of each of their upkeeps.
 */
class OniPossessionScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveUpkeep(pick: String? = null) {
        var guard = 0
        while (guard++ < 20) {
            when (val decision = getPendingDecision()) {
                is SelectCardsDecision -> {
                    val chosen = if (pick != null) {
                        decision.options.filter { state.getEntity(it)?.get<CardComponent>()?.name == pick }
                            .take(decision.minSelections)
                    } else decision.options.take(decision.minSelections)
                    selectCards(chosen)
                }
                null -> if (state.stack.isNotEmpty()) resolveStack() else break
                else -> error("unexpected decision $decision")
            }
        }
    }

    init {
        context("Oni Possession") {
            test("enchanted creature gets +3/+3, trample, and is only a Demon Spirit") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Oni Possession", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 5
                projected.getToughness(bears) shouldBe 5
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                projected.hasSubtype(bears, "Demon") shouldBe true
                projected.hasSubtype(bears, "Spirit") shouldBe true
                projected.hasSubtype(bears, "Bear") shouldBe false
                projected.isCreature(bears) shouldBe true
            }

            test("at your upkeep you sacrifice a creature of your choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Oni Possession", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep(pick = "Hill Giant")
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Oni Possession") shouldBe true
            }

            test("sacrificing the only creature takes the enchanted creature and the Aura goes too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Oni Possession", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Oni Possession") shouldBe false
            }

            test("the Aura controller sacrifices, even when enchanting an opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Oni Possession", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
