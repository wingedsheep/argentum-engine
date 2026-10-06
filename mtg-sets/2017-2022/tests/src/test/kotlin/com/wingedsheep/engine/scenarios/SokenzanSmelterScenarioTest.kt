package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sokenzan Smelter (NEO #164) — {1}{R} Creature — Goblin Artificer 2/2.
 *
 *   At the beginning of combat on your turn, you may pay {1} and sacrifice an artifact. If you do,
 *   create a 3/1 red Construct artifact creature token with haste.
 *
 * Covers the two-part optional cost: paying both makes the token, declining makes nothing, and
 * with no artifact to sacrifice the "yes" is never offered (no {1} is spent for nothing).
 */
class SokenzanSmelterScenarioTest : ScenarioTestBase() {

    init {
        context("Sokenzan Smelter") {

            test("paying {1} and sacrificing an artifact creates a 3/1 haste Construct") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sokenzan Smelter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Millstone")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                val select = game.getPendingDecision()
                select.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(game.findPermanent("Millstone")!!))
                game.resolveStack()

                withClue("the chosen artifact was sacrificed, the other stays") {
                    game.isInGraveyard(1, "Millstone") shouldBe true
                    (game.findPermanent("Ornithopter") != null) shouldBe true
                }
                withClue("the {1} was paid with the Mountain") {
                    (game.state.getEntity(game.findPermanent("Mountain")!!)?.has<TappedComponent>() == true) shouldBe true
                }
                withClue("one Construct token was created") {
                    game.findPermanents("Construct Token").size shouldBe 1
                }
            }

            test("declining pays nothing and creates no token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sokenzan Smelter", summoningSickness = false)
                    .withCardOnBattlefield(1, "Millstone")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                (game.findPermanent("Millstone") != null) shouldBe true
                (game.state.getEntity(game.findPermanent("Mountain")!!)?.has<TappedComponent>() == true) shouldBe false
                game.findPermanents("Construct Token").size shouldBe 0
            }

            test("with no artifact to sacrifice, the offer is skipped and no mana is spent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sokenzan Smelter", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe false
                (game.state.getEntity(game.findPermanent("Mountain")!!)?.has<TappedComponent>() == true) shouldBe false
                game.findPermanents("Construct Token").size shouldBe 0
            }
        }
    }
}
