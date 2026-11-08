package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Markov Enforcer (VOC #26) — {4}{R}{R} Creature — Vampire Soldier 6/6.
 *
 * "Whenever this creature or another Vampire you control enters, this creature fights up to one
 * target creature an opponent controls. Whenever a creature dealt damage by this creature this turn
 * dies, create a Blood token."
 *
 * The interaction worth proving: damage the Enforcer deals in a fight is recorded as damage dealt
 * by it, so the creature it fights and kills fires the Blood trigger.
 */
class MarkovEnforcerScenarioTest : ScenarioTestBase() {

    init {
        context("Markov Enforcer") {

            test("entering itself, it fights and the creature it kills yields a Blood token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Markov Enforcer")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Markov Enforcer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Markov Enforcer") shouldBe true
                game.findPermanents("Blood") shouldHaveSize 1
            }

            test("another Vampire you control entering makes the Enforcer fight") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Markov Enforcer")
                    .withCardInHand(1, "Vampire Interloper")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Vampire Interloper").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Vampire Interloper") shouldBe true
                game.findPermanents("Blood") shouldHaveSize 1
            }

            test("choosing no target means no fight and no Blood token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Markov Enforcer")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Markov Enforcer").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
                game.findPermanents("Blood").shouldBeEmpty()
            }
        }
    }
}
