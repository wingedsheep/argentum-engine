package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tezzeret, Cruel Machinist (M19 #286, {4}{U}{U} planeswalker, loyalty 4).
 *
 *   +1: Draw a card.
 *   0: Until your next turn, target artifact you control becomes a 5/5 creature in addition to its
 *      other types.
 *   −7: Put any number of cards from your hand onto the battlefield face down. They're 5/5
 *       artifact creatures.
 */
class TezzeretCruelMachinistScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Tezzeret, Cruel Machinist")!!.script.activatedAbilities[index]

    private fun activate(game: TestGame, tezz: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id, sourceId = tezz, abilityId = ability(index).id, targets = targets,
            )
        ).error shouldBe null
        game.resolveStack()
    }

    private fun base() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Tezzeret, Cruel Machinist")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("+1 draws a card") {
            val game = base().build()
            val tezz = game.findPermanent("Tezzeret, Cruel Machinist")!!
            seedLoyalty(game, tezz, 4)
            val handBefore = game.handSize(1)

            activate(game, tezz, 0)

            loyalty(game, tezz) shouldBe 5
            game.handSize(1) shouldBe handBefore + 1
        }

        test("0 makes an artifact a 5/5 creature until your next turn, keeping its types") {
            val game = base().withCardOnBattlefield(1, "Millstone").build()
            val tezz = game.findPermanent("Tezzeret, Cruel Machinist")!!
            val millstone = game.findPermanent("Millstone")!!
            seedLoyalty(game, tezz, 4)

            activate(game, tezz, 1, listOf(ChosenTarget.Permanent(millstone)))

            loyalty(game, tezz) shouldBe 4
            game.state.projectedState.isCreature(millstone) shouldBe true
            game.state.projectedState.hasType(millstone, "ARTIFACT") shouldBe true
            game.state.projectedState.getPower(millstone) shouldBe 5
            game.state.projectedState.getToughness(millstone) shouldBe 5

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("still a creature through the end of this turn") {
                game.state.projectedState.isCreature(millstone) shouldBe true
            }
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("still a creature during the opponent's turn") {
                game.state.projectedState.isCreature(millstone) shouldBe true
            }
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("stops being a creature as your next turn begins") {
                game.state.projectedState.isCreature(millstone) shouldBe false
            }
        }

        test("−7 puts the chosen hand cards onto the battlefield face down as 5/5 artifact creatures") {
            val game = base()
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Island")
                .build()
            val tezz = game.findPermanent("Tezzeret, Cruel Machinist")!!
            seedLoyalty(game, tezz, 8)
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            val shock = game.findCardsInHand(1, "Shock").single()
            val island = game.findCardsInHand(1, "Island").single()

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tezz, abilityId = ability(2).id)
            ).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as SelectCardsDecision
            withClue("any card in hand may be chosen, even an instant") {
                decision.options.containsAll(listOf(bears, shock, island)) shouldBe true
            }
            game.selectCards(listOf(bears, shock)).error shouldBe null
            game.resolveStack()

            loyalty(game, tezz) shouldBe 1
            game.state.getZone(game.player1Id, Zone.HAND).contains(island) shouldBe true
            val projected = game.state.projectedState
            for (id in listOf(bears, shock)) {
                game.state.getBattlefield().contains(id) shouldBe true
                game.state.getEntity(id)!!.has<FaceDownComponent>() shouldBe true
                projected.isCreature(id) shouldBe true
                projected.hasType(id, "ARTIFACT") shouldBe true
                projected.getPower(id) shouldBe 5
                projected.getToughness(id) shouldBe 5
                projected.getSubtypes(id) shouldBe emptySet()
                projected.getColors(id) shouldBe emptySet()
            }
        }
    }
}
