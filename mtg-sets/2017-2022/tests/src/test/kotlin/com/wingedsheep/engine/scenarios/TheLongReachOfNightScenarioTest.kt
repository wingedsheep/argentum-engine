package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Long Reach of Night // Animus of Night's Reach (NEO #109).
 *
 * Chapters I and II: each opponent sacrifices a creature unless they discard a card — the opponent,
 * not the Saga's controller, both makes the discard choice and sacrifices. Chapter III flips the Saga
 * into Animus of Night's Reach, which gets +X/+0 on attack for each creature card in the defending
 * player's graveyard.
 */
class TheLongReachOfNightScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.toMyNextMain() {
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
        }

        context("The Long Reach of Night") {

            test("discard spares the creature, declining sacrifices it, and the Animus grows from the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Long Reach of Night")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .apply { repeat(8) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "The Long Reach of Night").error shouldBe null
                game.resolveStack()

                withClue("chapter I: the opponent is offered a discard from their own hand") {
                    val decision = game.getPendingDecision()
                    decision.shouldBeInstanceOf<SelectCardsDecision>()
                    decision.playerId shouldBe game.player2Id
                    decision.options shouldContainExactlyInAnyOrder game.findCardsInHand(2, "Lightning Bolt")
                    game.selectCards(game.findCardsInHand(2, "Lightning Bolt"))
                }
                game.resolveStack()
                withClue("discarding spared the Grizzly Bears") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                game.toMyNextMain()
                withClue("chapter II: the opponent declines to discard") {
                    val decision = game.getPendingDecision()
                    decision.shouldBeInstanceOf<SelectCardsDecision>()
                    decision.playerId shouldBe game.player2Id
                    game.skipSelection()
                }
                // The sacrifice itself may ask the opponent which creature; they only have one.
                (game.getPendingDecision() as? SelectCardsDecision)?.let { pick ->
                    pick.playerId shouldBe game.player2Id
                    game.selectCards(listOf(pick.options.first()))
                }
                game.resolveStack()
                withClue("declining meant sacrificing the Grizzly Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                game.toMyNextMain()
                withClue("chapter III returned the Saga transformed") {
                    game.findPermanent("The Long Reach of Night") shouldBe null
                    game.isOnBattlefield("Animus of Night's Reach") shouldBe true
                }

                // Animus re-entered on our last turn; on the next one it can attack.
                game.toMyNextMain()
                val animus = game.findPermanent("Animus of Night's Reach")!!
                game.state.projectedState.getPower(animus) shouldBe 0

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Animus of Night's Reach" to 2)).error shouldBe null
                game.resolveStack()

                withClue("+1/+0 per creature card in the defender's graveyard (Hill Giant, Grizzly Bears — not the Bolt)") {
                    game.state.projectedState.getPower(animus) shouldBe 2
                    game.state.projectedState.getToughness(animus) shouldBe 4
                }
            }
        }
    }
}
