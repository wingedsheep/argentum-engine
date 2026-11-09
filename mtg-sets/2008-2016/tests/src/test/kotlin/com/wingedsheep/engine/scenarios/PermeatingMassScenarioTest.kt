package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Permeating Mass (EMN #165, {G}, 1/3 Spirit).
 *
 *   Whenever this creature deals combat damage to a creature, that creature becomes a copy of
 *   this creature.
 *
 * Rulings (2016-07-13): a creature dealt lethal damage in combat dies before it becomes a copy;
 * a creature with 3 or more damage marked on it that becomes a copy is destroyed; effects already
 * applied to the damaged creature keep applying, and the copy has the permeating ability itself.
 * The source-dies-in-the-same-combat ruling is pinned in `TriggerSourceDepartedBeforeStackingTest`.
 */
class PermeatingMassScenarioTest : ScenarioTestBase() {

    private fun blockedBy(blocker: String, blockerDamage: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Permeating Mass")
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        if (blockerDamage > 0) {
            val id = game.findPermanent(blocker)!!
            game.state = game.state.updateEntity(id) { it.with(DamageComponent(blockerDamage)) }
        }
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Permeating Mass" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf(blocker to listOf("Permeating Mass"))).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        game.resolveStack()
        return game
    }

    init {
        test("the damaged blocker becomes a Permeating Mass with the permeating ability") {
            val game = blockedBy("Grizzly Bears")
            val copies = game.findPermanents("Permeating Mass")
            copies.size shouldBe 2
            val copy = copies.single { game.state.getEntity(it)!!.get<CardComponent>()!!.ownerId == game.player2Id }
            withClue("a 1/3 Spirit that keeps its own controller and the Mass's triggered ability") {
                game.state.projectedState.getPower(copy) shouldBe 1
                game.state.projectedState.getToughness(copy) shouldBe 3
                game.state.projectedState.getController(copy) shouldBe game.player2Id
                game.state.getEntity(copy)!!.get<CardComponent>()!!.typeLine.toString() shouldBe "Creature — Spirit"
            }
        }

        test("a creature dealt lethal damage dies before it can become a copy") {
            val game = blockedBy("Savannah Lions")
            game.findPermanent("Savannah Lions") shouldBe null
            game.findPermanents("Permeating Mass").size shouldBe 1
        }

        test("a creature with 3 or more damage marked on it is destroyed once it becomes a copy") {
            val game = blockedBy("Giant Spider", blockerDamage = 2)
            withClue("the 2/4 survived 3 damage, then became a 1/3 with 3 damage marked") {
                game.findPermanent("Giant Spider") shouldBe null
                game.findPermanents("Permeating Mass").size shouldBe 1
                game.state.getGraveyard(game.player2Id).size shouldBe 1
            }
        }
    }
}
