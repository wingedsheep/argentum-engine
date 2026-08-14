package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.BelladonnaTook
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Belladonna Took (HOB #4) — {1}{W} Legendary Creature — Halfling Citizen 2/2.
 *
 *   Whenever a token you control enters, you gain 1 life if this is the first time this ability has
 *   resolved this turn. If it's the second time, draw a card. If it's the third time, put a +1/+1
 *   counter on each creature you control.
 *
 * Two Raise the Alarms give four token-enters triggers in one turn, which walks the counter through
 * all three branches and one resolution past the end of them. The branches are exact-equality
 * checks, so the fourth resolution must do nothing at all rather than repeating the third.
 */
class BelladonnaTookScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)

    init {
        cardRegistry.register(BelladonnaTook)
        cardRegistry.register(RaiseTheAlarm)

        context("Belladonna Took") {

            test("first resolution gains life, second draws, third counters everyone, fourth does nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Belladonna Took")
                    .withCardInHand(1, "Raise the Alarm")
                    .withCardInHand(1, "Raise the Alarm")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val belladonna = game.findPermanent("Belladonna Took")!!
                val lifeBefore = game.getLifeTotal(1)
                val handBefore = game.handSize(1)

                // Two tokens enter → resolutions #1 (gain 1 life) and #2 (draw a card).
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                withClue("resolution #1 gained 1 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 1
                }
                withClue("resolution #2 drew a card (hand: -1 Raise the Alarm, +1 drawn)") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("resolution #3 hasn't happened yet, so nobody has a +1/+1 counter") {
                    plusOneCounters(game, belladonna) shouldBe 0
                }

                // Two more tokens → resolutions #3 (counter on each creature) and #4 (nothing).
                game.castSpell(1, "Raise the Alarm").error shouldBe null
                game.resolveStack()

                val tokens = game.findAllPermanents("Soldier Token")
                withClue("four Soldier tokens are on the battlefield") {
                    tokens.size shouldBe 4
                }
                withClue("resolution #3 put a +1/+1 counter on each creature you control") {
                    plusOneCounters(game, belladonna) shouldBe 1
                    tokens.forEach { plusOneCounters(game, it) shouldBe 1 }
                }
                withClue("resolution #4 matched no branch — exactly one counter each, not two") {
                    power(game, belladonna) shouldBe 3 // 2/2 plus a single +1/+1
                }
                withClue("no further life gain after the first resolution") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 1
                }
            }
        }
    }
}
