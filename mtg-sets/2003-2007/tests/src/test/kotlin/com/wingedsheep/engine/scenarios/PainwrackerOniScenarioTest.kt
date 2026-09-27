package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Painwracker Oni (CHK #136) — "At the beginning of your upkeep, sacrifice a creature if you don't
 * control an Ogre."
 *
 * The Ogre check is on resolution and counts only Ogres *you* control; the Oni itself is a legal
 * sacrifice when it is your only creature.
 */
class PainwrackerOniScenarioTest : ScenarioTestBase() {

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

    private fun board(ogreFor: Int?, bears: Boolean = true) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Painwracker Oni")
        .apply { if (bears) withCardOnBattlefield(1, "Grizzly Bears") }
        .apply { if (ogreFor != null) withCardOnBattlefield(ogreFor, "Deathcurse Ogre") }
        .withActivePlayer(1)
        .inPhase(Phase.BEGINNING, Step.UNTAP)
        .build()

    init {
        context("Painwracker Oni") {
            test("without an Ogre, you sacrifice a creature of your choice at upkeep") {
                val game = board(ogreFor = null)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep(pick = "Grizzly Bears")
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Painwracker Oni") shouldBe true
            }

            test("the Oni must sacrifice itself when it is your only creature") {
                val game = board(ogreFor = null, bears = false)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.isOnBattlefield("Painwracker Oni") shouldBe false
            }

            test("controlling an Ogre spares the sacrifice") {
                val game = board(ogreFor = 1)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Painwracker Oni") shouldBe true
                game.isOnBattlefield("Deathcurse Ogre") shouldBe true
            }

            test("an opponent's Ogre does not count") {
                val game = board(ogreFor = 2)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep(pick = "Grizzly Bears")
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Deathcurse Ogre") shouldBe true
            }
        }
    }
}
