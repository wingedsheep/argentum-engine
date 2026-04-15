package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Rimefire Torque.
 *
 * Card reference:
 * - Rimefire Torque ({1}{U}): Artifact
 *   "As Rimefire Torque enters, choose a creature type.
 *    Whenever a permanent you control of the chosen type enters,
 *    put a charge counter on Rimefire Torque.
 *    {T}, Remove three charge counters from Rimefire Torque:
 *    When you next cast an instant or sorcery spell this turn, copy it.
 *    You may choose new targets for the copy."
 */
class RimefireTorqueScenarioTest : ScenarioTestBase() {

    private fun TestGame.getChargeCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.CHARGE) ?: 0
    }

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Rimefire Torque - charge counter accumulation") {

            test("charge counter is added when a creature of the chosen type enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rimefire Torque")
                    .withCardInHand(1, "Elvish Warrior") // Elf creature
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castTorque = game.castSpell(1, "Rimefire Torque")
                castTorque.error shouldBe null
                game.resolveStack()
                game.chooseCreatureType("Elf")

                val torqueId = game.findPermanent("Rimefire Torque")!!
                withClue("Rimefire Torque should enter with no charge counters") {
                    game.getChargeCounters(torqueId) shouldBe 0
                }

                val castElf = game.castSpell(1, "Elvish Warrior")
                castElf.error shouldBe null
                game.resolveStack()

                withClue("Rimefire Torque should gain a charge counter when an Elf enters") {
                    game.getChargeCounters(torqueId) shouldBe 1
                }
            }

            test("no charge counter is added when a creature of a different type enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Rimefire Torque")
                    .withCardInHand(1, "Goblin Sky Raider") // Goblin creature
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castTorque = game.castSpell(1, "Rimefire Torque")
                castTorque.error shouldBe null
                game.resolveStack()
                game.chooseCreatureType("Elf")

                val torqueId = game.findPermanent("Rimefire Torque")!!

                val castGoblin = game.castSpell(1, "Goblin Sky Raider")
                castGoblin.error shouldBe null
                game.resolveStack()

                withClue("Rimefire Torque should not gain a charge counter when a Goblin enters (chose Elf)") {
                    game.getChargeCounters(torqueId) shouldBe 0
                }
            }
        }
    }
}
