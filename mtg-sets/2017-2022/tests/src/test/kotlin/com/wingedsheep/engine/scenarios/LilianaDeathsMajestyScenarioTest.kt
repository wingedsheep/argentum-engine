package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.LilianaDeathsMajesty
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Liliana, Death's Majesty (AKH #97, {3}{B}{B}, Loyalty 5).
 *
 *   +1: Create a 2/2 black Zombie creature token. Mill two cards.
 *   −3: Return target creature card from your graveyard to the battlefield. That creature is a
 *       black Zombie in addition to its other colors and types.
 *   −7: Destroy all non-Zombie creatures.
 *
 * The −3 targets a card in the graveyard, moves it, and then applies two permanent continuous
 * grants to the object that arrived — the test proves the grants land on the returned creature
 * and are additive (it stays green and a Bear).
 */
class LilianaDeathsMajestyScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(LilianaDeathsMajesty))

        context("Liliana, Death's Majesty") {

            test("+1 creates a 2/2 black Zombie and mills two") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death's Majesty")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death's Majesty")!!
                setLoyalty(game, liliana, 5)
                activate(game, liliana, index = 0)
                game.resolveStack()

                withClue("a Zombie token was created") { (game.findPermanent("Zombie Token") != null) shouldBe true }
                withClue("two cards were milled") { game.state.getGraveyard(game.player1Id).size shouldBe 2 }
                withClue("+1 took Liliana to 6") { loyalty(game, liliana) shouldBe 6 }
            }

            test("−3 returns the creature as a black Zombie in addition to its colors and types") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death's Majesty")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death's Majesty")!!
                setLoyalty(game, liliana, 5)
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                activate(
                    game, liliana, index = 1,
                    targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                )
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                withClue("it is black and still green") {
                    projected.getColors(returned) shouldContainAll setOf("BLACK", "GREEN")
                }
                withClue("it is a Zombie and still a Bear") {
                    projected.getSubtypes(returned) shouldContainAll setOf("Zombie", "Bear")
                }
                withClue("−3 took Liliana to 2") { loyalty(game, liliana) shouldBe 2 }
            }

            test("−7 destroys non-Zombie creatures and spares Zombies") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death's Majesty")
                    .withCardOnBattlefield(1, "Gravedigger")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death's Majesty")!!
                setLoyalty(game, liliana, 7)
                activate(game, liliana, index = 2)
                game.resolveStack()

                withClue("the Zombie survives") { game.isOnBattlefield("Gravedigger") shouldBe true }
                withClue("non-Zombies are destroyed") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Savannah Lions") shouldBe true
                }
            }

            test("a creature the −3 made a Zombie survives the −7") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Liliana, Death's Majesty")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val liliana = game.findPermanent("Liliana, Death's Majesty")!!
                setLoyalty(game, liliana, 5)
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                activate(
                    game, liliana, index = 1,
                    targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                )
                game.resolveStack()

                setLoyalty(game, liliana, 7)
                activate(game, liliana, index = 2)
                game.resolveStack()

                withClue("the Bear is a Zombie only through the −3, and survives") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("the non-Zombie is destroyed") { game.isInGraveyard(2, "Savannah Lions") shouldBe true }
            }
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        val ability = cardRegistry.getCard("Liliana, Death's Majesty")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
                .without<AbilityActivatedThisTurnComponent>()
        }
    }
}
