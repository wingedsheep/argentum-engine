package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Barbara Wright (WHO #14) — {1}{W} Legendary Creature — Human Advisor, 1/3.
 *
 *   History Teacher — Sagas you control have read ahead.
 *   Doctor's companion
 *
 * Granted read ahead synthesizes the cast Saga's "choose a chapter" entry choice (CR 702.155): the
 * Saga starts with that many lore counters and the chapters it skipped don't trigger. An opponent's
 * Saga isn't one "you control". The other entry seams are pinned by GrantedReadAheadScenarioTest.
 */
class BarbaraWrightScenarioTest : ScenarioTestBase() {

    init {
        context("Barbara Wright") {

            fun TestGame.lore(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

            fun TestGame.chooseChapter(chapter: Int) {
                val decision = getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                decision.playerId shouldBe player1Id
                decision.maxValue shouldBe 3
                submitDecision(NumberChosenResponse(decision.id, chapter)).error shouldBe null
            }

            test("a Saga you cast starts at the chosen chapter and the skipped chapter doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardInHand(1, "History of Benalia")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "History of Benalia").error shouldBe null
                game.resolveStack()
                game.chooseChapter(2)
                game.resolveStack()

                val saga = game.findPermanent("History of Benalia")!!
                withClue("entered with two lore counters") { game.lore(saga) shouldBe 2 }
                withClue("the Saga has read ahead while Barbara grants it") {
                    game.state.projectedState.hasKeyword(saga, Keyword.READ_AHEAD) shouldBe true
                }
                withClue("only chapter II triggered: one Knight, not two") {
                    game.findPermanents("Knight Token").size shouldBe 1
                }
            }

            test("choosing chapter III skips both token chapters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardInHand(1, "History of Benalia")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "History of Benalia").error shouldBe null
                game.resolveStack()
                game.chooseChapter(3)
                game.resolveStack()

                withClue("no Knight was made") { game.findPermanents("Knight Token").size shouldBe 0 }
                withClue("chapter III resolved, so the Saga was sacrificed") {
                    game.findPermanent("History of Benalia") shouldBe null
                }
            }

            test("an opponent's Saga doesn't get read ahead") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Barbara Wright")
                    .withCardInHand(2, "History of Benalia")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "History of Benalia").error shouldBe null
                game.resolveStack()

                withClue("no chapter question") { (game.getPendingDecision() is ChooseNumberDecision) shouldBe false }
                val saga = game.findPermanent("History of Benalia")!!
                game.lore(saga) shouldBe 1
                game.findPermanents("Knight Token").size shouldBe 1
            }
        }
    }
}
