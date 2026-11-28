package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Devil's Play (ISD #140) — {X}{R} Sorcery.
 *
 *   Devil's Play deals X damage to any target.
 *   Flashback {X}{R}{R}{R}
 */
class DevilsPlayScenarioTest : ScenarioTestBase() {

    init {
        test("deals X damage to the target and goes to the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Devil's Play")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInHand(1, "Devil's Play").single()
            val result = game.execute(
                CastSpell(game.player1Id, card, listOf(ChosenTarget.Player(game.player2Id)), xValue = 3)
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.isInGraveyard(1, "Devil's Play") shouldBe true
        }

        test("flashback {X}{R}{R}{R} casts it from the graveyard and exiles it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Devil's Play")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInGraveyard(1, "Devil's Play").single()
            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    card,
                    listOf(ChosenTarget.Player(game.player2Id)),
                    xValue = 2,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                )
            )
            withClue("flashback cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.isInExile(1, "Devil's Play") shouldBe true
            game.isInGraveyard(1, "Devil's Play") shouldBe false
        }
    }
}
