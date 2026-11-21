package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Goblin Test Pilot (DGM #74) — {1}{U}{R} 0/2 flier, "{T}: This creature deals 2
 * damage to any target chosen at random."
 *
 * The engine draws the target as the ability is activated: the player sends a bare activation,
 * nobody is asked, the draw follows the game's seeded RNG, and every legal target — both players,
 * the Pilot itself, its controller's creatures — is in it, while a hexproof creature is not.
 */
class GoblinTestPilotScenarioTest : ScenarioTestBase() {

    private val pingId get() = cardRegistry.getCard("Goblin Test Pilot")!!.activatedAbilities.single().id

    private fun board(seed: Long, opponentCreatures: List<String> = emptyList(), ownCreatures: List<String> = emptyList()): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withRngSeed(seed)
            .withCardOnBattlefield(1, "Goblin Test Pilot")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        opponentCreatures.forEach { builder.withCardOnBattlefield(2, it) }
        ownCreatures.forEach { builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    /** Activates with no target and resolves it; returns what took the 2 damage. */
    private fun TestGame.ping(): String {
        val pilot = findPermanent("Goblin Test Pilot")!!
        execute(ActivateAbility(player1Id, pilot, pingId)).error shouldBe null
        withClue("the random target is drawn, never asked for") { hasPendingDecision() shouldBe false }
        resolveStack()
        state.stack.isEmpty() shouldBe true
        return when {
            getLifeTotal(1) == 18 -> "Player1"
            getLifeTotal(2) == 18 -> "Player2"
            isInGraveyard(1, "Goblin Test Pilot") -> "Goblin Test Pilot"
            isInGraveyard(1, "Grizzly Bears") -> "Grizzly Bears"
            isInGraveyard(2, "Hill Giant") || (findPermanent("Hill Giant")?.let { damageOn(it) } == 2) -> "Hill Giant"
            else -> "nothing"
        }
    }

    private fun TestGame.damageOn(id: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(id)?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()?.amount ?: 0

    init {
        context("Goblin Test Pilot") {

            test("deals 2 damage to one legal target drawn at random, its own side included") {
                val hit = (1L..40L).map { seed ->
                    val game = board(seed, opponentCreatures = listOf("Hill Giant"), ownCreatures = listOf("Grizzly Bears"))
                    val target = game.ping()
                    withClue("seed $seed: exactly one thing took the damage") { target shouldNotBe "nothing" }
                    target
                }.toSet()
                withClue("every kind of legal target comes up across seeds") {
                    hit shouldContainAll listOf("Player1", "Player2", "Goblin Test Pilot", "Grizzly Bears", "Hill Giant")
                }
            }

            test("the same seed draws the same target") {
                board(7L, listOf("Hill Giant"), listOf("Grizzly Bears")).ping() shouldBe
                    board(7L, listOf("Hill Giant"), listOf("Grizzly Bears")).ping()
            }

            test("a hexproof creature is never drawn") {
                (1L..16L).forEach { seed ->
                    val game = board(seed, opponentCreatures = listOf("Gladecover Scout"))
                    game.ping()
                    withClue("seed $seed") {
                        val scout = game.findPermanent("Gladecover Scout")!!
                        game.damageOn(scout) shouldBe 0
                    }
                }
            }

            test("is offered without a target to pick, and a client-chosen target is refused") {
                val game = board(1L)
                val pilot = game.findPermanent("Goblin Test Pilot")!!
                val offered = game.getLegalActions(1).single { (it.action as? ActivateAbility)?.sourceId == pilot }
                offered.requiresTargets shouldBe false
                val aimed = game.execute(ActivateAbility(game.player1Id, pilot, pingId, targets = listOf(ChosenTarget.Player(game.player2Id))))
                aimed.error shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
