package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.VraskaBetrayalsSting
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vraska, Betrayal's Sting (ONE #115, {4}{B}{B/P}, loyalty 6).
 *
 *   0: You draw a card and lose 1 life. Proliferate.
 *   −2: Target creature becomes a Treasure artifact with "{T}, Sacrifice this artifact: Add one
 *       mana of any color" and loses all other card types and abilities.
 *   −9: If target player has fewer than nine poison counters, they get a number of poison
 *       counters equal to the difference.
 */
class VraskaBetrayalsStingScenarioTest : ScenarioTestBase() {

    private val zero = VraskaBetrayalsSting.activatedAbilities[0].id
    private val minusTwo = VraskaBetrayalsSting.activatedAbilities[1].id
    private val minusNine = VraskaBetrayalsSting.activatedAbilities[2].id

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board(): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Vraska, Betrayal's Sting")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Wind Drake")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val vraska = game.findPermanent("Vraska, Betrayal's Sting")!!
        return game to vraska
    }

    init {
        test("0: draw a card, lose 1 life, then proliferate onto Vraska and a poisoned opponent") {
            val (game, vraska) = board()
            seed(game, game.player2Id, CounterType.POISON, 2)

            game.execute(ActivateAbility(game.player1Id, vraska, zero)).error shouldBe null
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 10) {
                if (game.hasPendingDecision()) game.selectCards(listOf(vraska, game.player2Id))
                else game.resolveStack()
            }

            game.handSize(1) shouldBe 1
            game.getLifeTotal(1) shouldBe 19
            count(game, vraska, CounterType.LOYALTY) shouldBe 7
            count(game, game.player2Id, CounterType.POISON) shouldBe 3
        }

        test("−2 turns a creature into a Treasure artifact that keeps its color and loses its abilities") {
            val (game, vraska) = board()
            val drake = game.findPermanent("Wind Drake")!!

            game.execute(
                ActivateAbility(game.player1Id, vraska, minusTwo, targets = listOf(ChosenTarget.Permanent(drake)))
            ).error shouldBe null
            game.resolveStack()

            count(game, vraska, CounterType.LOYALTY) shouldBe 4
            val projected = game.state.projectedState
            withClue("only a Treasure artifact now") {
                projected.isCreature(drake) shouldBe false
                projected.hasType(drake, "ARTIFACT") shouldBe true
                projected.getSubtypes(drake) shouldBe setOf("Treasure")
            }
            withClue("its printed flying is gone") {
                projected.hasKeyword(drake, Keyword.FLYING) shouldBe false
            }
            withClue("the ruling keeps its color — the ability says nothing about colour") {
                projected.getColors(drake) shouldBe setOf("BLUE")
            }
            withClue("it carries the Treasure sac-for-mana ability") {
                game.state.grantedActivatedAbilities.any { it.entityId == drake && it.ability.isManaAbility } shouldBe true
            }
        }

        test("−9 tops a player up to nine poison counters, and does nothing at nine or more") {
            val (game, vraska) = board()
            seed(game, vraska, CounterType.LOYALTY, 3) // the builder already gave her 6
            seed(game, game.player2Id, CounterType.POISON, 3)

            game.execute(
                ActivateAbility(game.player1Id, vraska, minusNine, targets = listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            count(game, game.player2Id, CounterType.POISON) shouldBe 9
            count(game, vraska, CounterType.LOYALTY) shouldBe 0

            val (again, vraska2) = board()
            seed(again, vraska2, CounterType.LOYALTY, 3)
            seed(again, again.player1Id, CounterType.POISON, 9)
            again.execute(
                ActivateAbility(again.player1Id, vraska2, minusNine, targets = listOf(ChosenTarget.Player(again.player1Id)))
            ).error shouldBe null
            again.resolveStack()
            count(again, again.player1Id, CounterType.POISON) shouldBe 9
        }
    }
}
