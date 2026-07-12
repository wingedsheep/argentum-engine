package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Wedding Security (VOW #299) — {3}{B}{B} Creature — Vampire Soldier, 4/4.
 *
 *   Whenever this creature attacks, you may sacrifice a Blood token. If you do, put a +1/+1
 *   counter on this creature and draw a card.
 *
 * Exercises the attack-trigger gated Blood sacrifice: paying by sacrificing the Blood token adds
 * a +1/+1 counter and draws a card; declining leaves the attacker unbuffed.
 */
class WeddingSecurityScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Wedding Security") {

            test("attacking and sacrificing a Blood token adds a +1/+1 counter and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Security", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val security = game.findPermanent("Wedding Security")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wedding Security" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger offers a yes/no to sacrifice the Blood token") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("the Blood token was sacrificed") {
                    game.findPermanents("Blood").size shouldBe 0
                }
                withClue("Wedding Security has a +1/+1 counter") {
                    plusOneCounters(game, security) shouldBe 1
                }
                withClue("a card was drawn") {
                    game.isInHand(1, "Plains") shouldBe true
                }
            }

            test("declining the sacrifice leaves Wedding Security unbuffed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Security", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val security = game.findPermanent("Wedding Security")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wedding Security" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("the Blood token is not sacrificed") {
                    game.findPermanents("Blood").size shouldBe 1
                }
                withClue("Wedding Security has no counter") {
                    plusOneCounters(game, security) shouldBe 0
                }
                withClue("no card was drawn") {
                    game.isInHand(1, "Plains") shouldBe false
                }
            }
        }
    }
}
