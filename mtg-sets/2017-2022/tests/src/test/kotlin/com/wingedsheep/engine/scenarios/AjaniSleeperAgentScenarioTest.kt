package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmu.cards.AjaniSleeperAgent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ajani, Sleeper Agent (DMU). The −3 is the first *activated* counter division: its split is
 * announced with the activation (CR 601.2d) and rides on the stack object, so the legal action
 * must ask for it and a target removed in response simply loses its share.
 */
class AjaniSleeperAgentScenarioTest : ScenarioTestBase() {

    private val plusOne = AjaniSleeperAgent.activatedAbilities[0].id
    private val minusThree = AjaniSleeperAgent.activatedAbilities[1].id
    private val minusSix = AjaniSleeperAgent.activatedAbilities[2].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("+1 puts a revealed creature card into your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            seedLoyalty(game, ajani, 4)

            game.execute(ActivateAbility(game.player1Id, ajani, plusOne)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            counters(game, ajani, CounterType.LOYALTY) shouldBe 5
        }

        test("+1 offers to bottom a revealed card that is neither a creature nor a planeswalker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withCardInLibrary(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            seedLoyalty(game, ajani, 4)
            val bolt = game.state.getZone(game.player1Id, Zone.LIBRARY).single()

            game.execute(ActivateAbility(game.player1Id, ajani, plusOne)).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bolt)).error shouldBe null
            game.isInHand(1, "Lightning Bolt") shouldBe false
            game.state.getZone(game.player1Id, Zone.LIBRARY) shouldBe listOf(bolt)
        }

        test("−3 asks for the division at activation, honors it, and gives every target vigilance") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            seedLoyalty(game, ajani, 4)
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!

            val action = game.getLegalActions(1).single {
                (it.action as? ActivateAbility)?.abilityId == minusThree
            }
            action.requiresDamageDistribution shouldBe true
            action.totalDamageToDistribute shouldBe 3
            action.distributionUnit shouldBe "+1/+1 counter"

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = ajani,
                    abilityId = minusThree,
                    targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(lions)),
                    damageDistribution = mapOf(bears to 1, lions to 2),
                )
            ).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            counters(game, lions, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            game.state.projectedState.hasKeyword(lions, Keyword.VIGILANCE) shouldBe true
            counters(game, ajani, CounterType.LOYALTY) shouldBe 1
        }

        test("−6 emblem gives target opponent two poison counters whenever you cast a creature spell") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            seedLoyalty(game, ajani, 6)

            game.execute(ActivateAbility(game.player1Id, ajani, minusSix)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Ajani, Sleeper Agent") shouldBe null

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            counters(game, game.player2Id, CounterType.POISON) shouldBe 2
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
    }
}
