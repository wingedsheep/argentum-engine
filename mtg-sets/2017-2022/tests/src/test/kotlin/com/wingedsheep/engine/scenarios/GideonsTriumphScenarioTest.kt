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
 * Gideon's Triumph — target opponent sacrifices a creature of their choice that attacked or blocked
 * this turn; two of those instead if you control a Gideon planeswalker (checked on resolution).
 */
class GideonsTriumphScenarioTest : ScenarioTestBase() {

    private fun blockedBoard(withGideon: Boolean): TestGame {
        val builder = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(1, "Glory Seeker")
            .withCardInHand(1, "Gideon's Triumph")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(2, "Craw Wurm")   // blocks
            .withCardOnBattlefield(2, "Hill Giant")      // blocks
            .withCardOnBattlefield(2, "Grizzly Bears")       // neither attacked nor blocked
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Forest")
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        if (withGideon) builder.withCardOnBattlefield(1, "Gideon, Champion of Justice")
        val game = builder.build()

        game.declareAttackers(mapOf("Savannah Lions" to 2, "Glory Seeker" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(
            mapOf(
                "Craw Wurm" to listOf("Savannah Lions"),
                "Hill Giant" to listOf("Glory Seeker"),
            )
        ).error shouldBe null
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        return game
    }

    init {
        test("opponent sacrifices one creature of their choice among those that blocked this turn") {
            val game = blockedBoard(withGideon = false)

            game.castSpellTargetingPlayer(1, "Gideon's Triumph", 2).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the target opponent chooses") {
                decision.playerId shouldBe game.player2Id
            }
            withClue("only creatures that attacked or blocked this turn are offered") {
                decision.options shouldContainExactlyInAnyOrder listOf(
                    game.findPermanent("Craw Wurm")!!,
                    game.findPermanent("Hill Giant")!!,
                )
            }
            game.selectCards(listOf(game.findPermanent("Hill Giant")!!))

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("with a Gideon planeswalker the opponent sacrifices two of those creatures") {
            val game = blockedBoard(withGideon = true)

            game.castSpellTargetingPlayer(1, "Gideon's Triumph", 2).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            if (decision is SelectCardsDecision) {
                game.selectCards(
                    listOf(game.findPermanent("Craw Wurm")!!, game.findPermanent("Hill Giant")!!)
                )
            }

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            withClue("a creature that neither attacked nor blocked is never sacrificed") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
