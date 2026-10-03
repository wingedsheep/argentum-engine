package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deep Analysis (TOR #36, reprinted in MH3) — target player draws two cards; flashback costs
 * {1}{U} plus 3 life and exiles the card.
 */
class DeepAnalysisScenarioTest : ScenarioTestBase() {

    init {
        fun builder(life: Int = 20): ScenarioBuilder {
            var b = scenario()
                .withPlayers("Player", "Opponent")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLifeTotal(1, life)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { b = b.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island") }
            return b
        }

        context("Deep Analysis") {
            test("cast from hand, the target player draws two cards") {
                val game = builder().withCardInHand(1, "Deep Analysis").build()
                val card = game.findCardsInHand(1, "Deep Analysis").single()

                val cast = game.execute(
                    CastSpell(game.player1Id, card, listOf(ChosenTarget.Player(game.player2Id)))
                )
                withClue("hand cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.handSize(2) shouldBe 2
                game.handSize(1) shouldBe 0
                game.getLifeTotal(1) shouldBe 20
                game.isInGraveyard(1, "Deep Analysis") shouldBe true
            }

            test("flashed back, it costs 3 life, draws two and is exiled") {
                val game = builder().withCardInGraveyard(1, "Deep Analysis").build()
                val card = game.findCardsInGraveyard(1, "Deep Analysis").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                withClue("flashback cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.handSize(1) shouldBe 2
                game.isInExile(1, "Deep Analysis") shouldBe true
            }

            test("with less than 3 life, flashback can't be cast") {
                val game = builder(life = 2).withCardInGraveyard(1, "Deep Analysis").build()
                val card = game.findCardsInGraveyard(1, "Deep Analysis").single()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        useAlternativeCost = true,
                        alternativeCostType = AlternativeCostType.FLASHBACK,
                    )
                )
                cast.error shouldNotBe null
                game.getLifeTotal(1) shouldBe 2
                game.isInGraveyard(1, "Deep Analysis") shouldBe true
            }
        }
    }
}
