package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Torment of Hailfire (HOU #77) — {X}{B}{B} Sorcery: "Repeat the following process X times. Each
 * opponent loses 3 life unless that player sacrifices a nonland permanent of their choice or
 * discards a card."
 *
 * The process repeats X times with the opponent choosing afresh each time, and an option the
 * opponent can no longer afford (lands don't count as nonland permanents) drops out.
 */
class TormentOfHailfireScenarioTest : ScenarioTestBase() {

    init {
        context("Torment of Hailfire") {

            test("each process is a fresh choice: sacrifice first, then lose life once the creature is gone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Torment of Hailfire")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withCardInHand(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Torment of Hailfire", xValue = 2).error shouldBe null
                game.resolveStack()

                val first = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("the opponent makes the choice") { first.playerId shouldBe game.player2Id }
                first.options.size shouldBe 3
                game.submitDecision(OptionChosenResponse(first.id, 0))
                val pick = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only the nonland permanent can be sacrificed") {
                    pick.options shouldBe listOf(game.findPermanent("Grizzly Bears")!!)
                }
                game.selectCards(pick.options)

                val second = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("no nonland permanent left: discard or lose life") { second.options.size shouldBe 2 }
                game.submitDecision(OptionChosenResponse(second.id, second.options.lastIndex))

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 17
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(2) shouldBe 1
            }

            test("an opponent with only lands and no cards in hand loses 3 life per process") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Torment of Hailfire")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Torment of Hailfire", xValue = 3).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision shouldBe null
                game.getLifeTotal(2) shouldBe 11
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
