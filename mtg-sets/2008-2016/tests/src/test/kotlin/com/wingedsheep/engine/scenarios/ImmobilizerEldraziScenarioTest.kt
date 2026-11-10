package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Immobilizer Eldrazi — {1}{R} 2/1 Eldrazi Drone. "Devoid. {2}{C}: Each creature with toughness
 * greater than its power can't block this turn."
 *
 * First card to put `ToughnessGreaterThanPower` in a group filter. Covers the comparison itself
 * (strictly greater: a 2/2 still blocks) and the ruling that the comparison is made as blockers are
 * declared, not as the ability resolves — a 3/3 pumped to 3/7 afterwards can't block.
 */
class ImmobilizerEldraziScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Immobilizer Eldrazi")!!.script.activatedAbilities.single().id
        val wallAbilityId = cardRegistry.getCard("Crenellated Wall")!!.script.activatedAbilities.single().id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Immobilizer Eldrazi")
            .withCardOnBattlefield(1, "Crenellated Wall")
            .withLandsOnBattlefield(1, "Wastes", 1)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.activate() {
            val eldrazi = findPermanent("Immobilizer Eldrazi")!!
            val result = execute(ActivateAbility(player1Id, eldrazi, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            resolveStack()
        }

        fun ScenarioTestBase.TestGame.attackWithEldrazi() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Immobilizer Eldrazi" to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }

        test("devoid: the red-costed Eldrazi is colorless") {
            val game = board()
            game.state.projectedState.getColors(game.findPermanent("Immobilizer Eldrazi")!!) shouldBe emptySet()
        }

        test("without the ability, a creature with toughness greater than its power can block") {
            val game = board()
            game.attackWithEldrazi()

            withClue("Wall of Wood (0/3) blocks normally") {
                game.declareBlockers(mapOf("Wall of Wood" to listOf("Immobilizer Eldrazi"))).error shouldBe null
            }
        }

        test("a creature with toughness greater than its power can't block") {
            val game = board()
            game.activate()
            game.attackWithEldrazi()

            withClue("Wall of Wood is 0/3, so it can't block") {
                game.declareBlockers(mapOf("Wall of Wood" to listOf("Immobilizer Eldrazi"))).error shouldNotBe null
            }
        }

        test("a creature whose toughness equals its power can still block") {
            val game = board()
            game.activate()
            game.attackWithEldrazi()

            withClue("Grizzly Bears is 2/2 — toughness isn't greater than power") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Immobilizer Eldrazi"))).error shouldBe null
            }
        }

        test("the comparison is made as blockers are declared, not as the ability resolves") {
            val game = board()
            game.activate()

            // After the ability resolved, pump the 3/3 Hill Giant to 3/7.
            val wall = game.findPermanent("Crenellated Wall")!!
            val giant = game.findPermanent("Hill Giant")!!
            val pump = game.execute(
                ActivateAbility(game.player1Id, wall, wallAbilityId, listOf(ChosenTarget.Permanent(giant)))
            )
            withClue("Crenellated Wall activation should succeed: ${pump.error}") { pump.error shouldBe null }
            game.resolveStack()
            game.state.projectedState.getToughness(giant) shouldBe 7

            game.attackWithEldrazi()

            withClue("Hill Giant was 3/3 at resolution but is 3/7 now, so it can't block") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Immobilizer Eldrazi"))).error shouldNotBe null
            }
        }
    }
}
