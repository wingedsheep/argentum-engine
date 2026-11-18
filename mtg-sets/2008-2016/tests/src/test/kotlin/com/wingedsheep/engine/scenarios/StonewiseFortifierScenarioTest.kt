package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stonewise Fortifier (JOU #27): "{4}{W}: Prevent all damage that would be dealt to this creature
 * by target creature this turn."
 *
 * The shield covers exactly one source (the targeted creature) and one recipient (the Fortifier):
 * another source still damages the Fortifier, and the targeted creature still damages anything
 * else. (The shield's end-of-turn expiry is pinned by SpecificSourcePreventionTest.)
 */
class StonewiseFortifierScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.activate(sourceName: String, target: ChosenTarget) {
        val source = findPermanent(sourceName)!!
        val abilityId = cardRegistry.getCard(sourceName)!!.activatedAbilities.first().id
        val result = execute(
            ActivateAbility(playerId = player1Id, sourceId = source, abilityId = abilityId, targets = listOf(target))
        )
        withClue("activating $sourceName") { result.error shouldBe null }
        resolveStack()
    }

    private fun board() = scenario().withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Stonewise Fortifier")
        .withCardOnBattlefield(1, "Prodigal Sorcerer")
        .withCardOnBattlefield(1, "Prodigal Pyromancer")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("damage from the targeted creature to the Fortifier is prevented") {
            val game = board()
            val fortifier = game.findPermanent("Stonewise Fortifier")!!
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!

            game.activate("Stonewise Fortifier", ChosenTarget.Permanent(sorcerer))
            game.activate("Prodigal Sorcerer", ChosenTarget.Permanent(fortifier))

            game.damageOn(fortifier) shouldBe 0
        }

        test("another source still damages the Fortifier") {
            val game = board()
            val fortifier = game.findPermanent("Stonewise Fortifier")!!
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!

            game.activate("Stonewise Fortifier", ChosenTarget.Permanent(sorcerer))
            game.activate("Prodigal Pyromancer", ChosenTarget.Permanent(fortifier))

            game.damageOn(fortifier) shouldBe 1
        }

        test("the targeted creature's damage to anything else is not prevented") {
            val game = board()
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!

            game.activate("Stonewise Fortifier", ChosenTarget.Permanent(sorcerer))
            game.activate("Prodigal Sorcerer", ChosenTarget.Player(game.player2Id))

            game.getLifeTotal(2) shouldBe 19
        }
    }
}
