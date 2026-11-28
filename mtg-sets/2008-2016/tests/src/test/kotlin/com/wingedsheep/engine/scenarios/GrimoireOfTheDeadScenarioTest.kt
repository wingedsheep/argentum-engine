package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Grimoire of the Dead (ISD #226) — {4} Legendary Artifact — Book.
 *
 *   {1}, {T}, Discard a card: Put a study counter on Grimoire of the Dead.
 *   {T}, Remove three study counters from Grimoire of the Dead and sacrifice it: Put all creature
 *   cards from all graveyards onto the battlefield under your control. They're black Zombies in
 *   addition to their other colors and types.
 *
 * Pins the mass return: creature cards from *both* graveyards (an artifact creature included) enter
 * under the activator's control as black Zombies on top of their own colours and types; noncreature
 * cards and the sacrificed Grimoire stay in the graveyard.
 */
class GrimoireOfTheDeadScenarioTest : ScenarioTestBase() {

    private val reanimateAbilityId = cardRegistry.getCard("Grimoire of the Dead")!!.activatedAbilities[1].id

    private fun TestGame.seedStudy(grimoire: EntityId, amount: Int) {
        state = state.updateEntity(grimoire) { it.with(CountersComponent().withAdded(CounterType.STUDY, amount)) }
    }

    private fun newGame(): TestGame {
        var builder = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Grimoire of the Dead")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(1, "Lightning Bolt")
            .withCardInGraveyard(2, "Hill Giant")
            .withCardInGraveyard(2, "Ornithopter")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Swamp") }
        return builder.build()
    }

    init {
        test("needs three study counters") {
            val game = newGame()
            val grimoire = game.findPermanent("Grimoire of the Dead")!!
            game.seedStudy(grimoire, 2)

            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = grimoire, abilityId = reanimateAbilityId)
            )
            withClue("two counters can't pay the cost") { result.error shouldNotBe null }
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("puts every creature card from all graveyards onto the battlefield as black Zombies under your control") {
            val game = newGame()
            val grimoire = game.findPermanent("Grimoire of the Dead")!!
            game.seedStudy(grimoire, 3)

            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = grimoire, abilityId = reanimateAbilityId)
            )
            withClue("activation: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("sacrificed as a cost, and not a creature card") {
                game.isInGraveyard(1, "Grimoire of the Dead") shouldBe true
            }
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true

            val projected = game.state.projectedState
            for (name in listOf("Grizzly Bears", "Hill Giant", "Ornithopter")) {
                val creature = game.findPermanent(name)
                withClue("$name returned") { creature shouldNotBe null }
                withClue("$name is under the activator's control") {
                    projected.getController(creature!!) shouldBe game.player1Id
                }
                withClue("$name is a Zombie in addition to its other types") {
                    projected.hasSubtype(creature!!, "Zombie") shouldBe true
                }
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            projected.getColors(bears) shouldBe setOf("GREEN", "BLACK")
            projected.hasSubtype(bears, "Bear") shouldBe true
            val giant = game.findPermanent("Hill Giant")!!
            projected.getColors(giant) shouldBe setOf("RED", "BLACK")
            projected.hasSubtype(giant, "Giant") shouldBe true
            withClue("a colorless artifact creature is simply black") {
                val thopter = game.findPermanent("Ornithopter")!!
                projected.getColors(thopter) shouldBe setOf("BLACK")
                projected.isCreature(thopter) shouldBe true
            }
        }
    }
}
