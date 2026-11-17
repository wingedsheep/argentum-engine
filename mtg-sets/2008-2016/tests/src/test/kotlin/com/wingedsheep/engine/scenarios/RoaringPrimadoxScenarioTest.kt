package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Roaring Primadox: at the beginning of your upkeep, return a creature you control to its owner's
 * hand. It isn't optional, and the Primadox itself is a legal choice.
 */
class RoaringPrimadoxScenarioTest : ScenarioTestBase() {
    init {
        test("your upkeep returns the creature you choose to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Roaring Primadox")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            withClue("only your creatures are offered, the Primadox included") {
                decision.options.toSet() shouldBe setOf(
                    game.findPermanent("Roaring Primadox")!!,
                    game.findPermanent("Grizzly Bears")!!,
                )
            }
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Roaring Primadox") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
