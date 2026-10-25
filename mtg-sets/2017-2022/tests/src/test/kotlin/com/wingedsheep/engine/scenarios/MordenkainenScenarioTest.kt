package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.Mordenkainen
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Mordenkainen (AFR #64, {4}{U}{U}, loyalty 5).
 *
 * The −2 Dog Illusion's P/T is a characteristic-defining ability that follows the hand size. The
 * −10 swaps hand and library simultaneously (nothing is drawn), shuffles, and leaves the player
 * with no maximum hand size plus an emblem badge.
 */
class MordenkainenScenarioTest : ScenarioTestBase() {

    private val name = "Mordenkainen"

    init {
        cardRegistry.register(listOf(Mordenkainen))

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("−2 Dog Illusion is twice your hand size, and tracks it") {
            val game = board()
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Island")
                .withCardInHand(1, "Plains")
                .withCardInLibrary(1, "Swamp")
                .build()
            val mordenkainen = game.findPermanent(name)!!
            setLoyalty(game, mordenkainen, 5)

            activate(game, mordenkainen, index = 1)
            game.resolveStack()

            val dog = game.state.getBattlefield().single { it != mordenkainen }
            val projected = game.state.projectedState
            withClue("three cards in hand: 6/6") {
                projected.getPower(dog) shouldBe 6
                projected.getToughness(dog) shouldBe 6
            }
            projected.getSubtypes(dog) shouldContainExactlyInAnyOrder setOf("Dog", "Illusion")

            val forest = game.state.getHand(game.player1Id).first()
            game.state = services.zones.moveToZone(game.state, forest, Zone.GRAVEYARD).state
            withClue("two cards in hand: 4/4") {
                game.state.projectedState.getPower(dog) shouldBe 4
                game.state.projectedState.getToughness(dog) shouldBe 4
            }
            loyalty(game, mordenkainen) shouldBe 3
        }

        test("−10 exchanges hand and library, shuffles, and removes maximum hand size") {
            val game = board()
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Island")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Plains")
                .build()
            val mordenkainen = game.findPermanent(name)!!
            val oldHand = game.state.getHand(game.player1Id).toList()
            val oldLibrary = game.state.getLibrary(game.player1Id).toList()
            setLoyalty(game, mordenkainen, 10)

            activate(game, mordenkainen, index = 2)
            game.resolveStack()

            game.state.getHand(game.player1Id) shouldContainExactlyInAnyOrder oldLibrary
            game.state.getLibrary(game.player1Id) shouldContainExactlyInAnyOrder oldHand
            MaximumHandSize.effective(
                game.state, game.player1Id, cardRegistry,
                services.conditionEvaluator, services.dynamicAmountEvaluator
            ) shouldBe null
            MaximumHandSize.effective(
                game.state, game.player2Id, cardRegistry,
                services.conditionEvaluator, services.dynamicAmountEvaluator
            ) shouldBe 7
            withClue("the player has a Mordenkainen emblem") {
                game.state.entities.values.count {
                    it.get<EmblemSourceComponent>()?.sourceName == name
                } shouldBe 1
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id)
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
