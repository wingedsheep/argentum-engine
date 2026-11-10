package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Devour in Flames — {2}{R} Sorcery. "As an additional cost to cast this spell, return a land you
 * control to its owner's hand. Devour in Flames deals 5 damage to target creature or planeswalker."
 */
class DevourinFlamesScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Devour in Flames")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("returning a land, it deals 5 damage to the target creature") {
            val game = board()
            val spell = game.findCardsInHand(1, "Devour in Flames").single()
            val giant = game.findPermanent("Hill Giant")!!
            val land = game.findPermanents("Mountain").first()

            val cast = game.execute(
                CastSpell(
                    game.player1Id, spell, listOf(ChosenTarget.Permanent(giant)),
                    additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(land)),
                )
            )
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("the returned Mountain is back in hand") {
                game.isInHand(1, "Mountain") shouldBe true
                game.findPermanents("Mountain").size shouldBe 3
            }
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("can't be cast without returning a land") {
            val game = board()
            val spell = game.findCardsInHand(1, "Devour in Flames").single()
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                CastSpell(
                    game.player1Id, spell, listOf(ChosenTarget.Permanent(giant)),
                    additionalCostPayment = AdditionalCostPayment(bouncedPermanents = emptyList()),
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
