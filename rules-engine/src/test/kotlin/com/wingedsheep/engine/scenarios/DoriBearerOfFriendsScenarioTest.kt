package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dori, Bearer of Friends (HOB #94) — {2}{R} Legendary Creature — Dwarf Warrior 3/2.
 *
 * "Trample
 *  When Dori enters, create a Treasure token."
 *
 * The counterpart to Long-Bodied Grey Dog's tapped Treasure — Dori's arrives untapped and is
 * usable the same turn.
 */
class DoriBearerOfFriendsScenarioTest : ScenarioTestBase() {

    init {
        context("Dori, Bearer of Friends") {

            test("it enters as a 3/2 trampler and makes an untapped Treasure") {
                val g = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dori, Bearer of Friends")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                g.castSpell(1, "Dori, Bearer of Friends").error shouldBe null
                g.resolveStack()

                val dori = g.findPermanent("Dori, Bearer of Friends")!!
                withClue("printed characteristics") {
                    g.state.projectedState.getPower(dori) shouldBe 3
                    g.state.projectedState.getToughness(dori) shouldBe 2
                    g.state.projectedState.hasKeyword(dori, Keyword.TRAMPLE) shouldBe true
                }

                val treasure = g.findPermanent("Treasure")
                    ?: error("no Treasure token was created")
                withClue("Dori's Treasure is not tapped") {
                    g.state.getEntity(treasure)?.get<TappedComponent>() shouldBe null
                }
            }
        }
    }
}
