package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.EmblemStaticAbilityComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dovin Baan (KLD #179, {2}{W}{U}, loyalty 3).
 *
 * +1: Until your next turn, up to one target creature gets -3/-0 and its activated abilities
 *     can't be activated.
 * −1: You gain 2 life and draw a card.
 * −7: You get an emblem with "Your opponents can't untap more than two permanents during their
 *     untap steps."
 */
class DovinBaanScenarioTest : ScenarioTestBase() {

    init {
        test("+1 shrinks and locks the creature through the opponent's turn, then wears off") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dovin Baan")
                .withCardOnBattlefield(2, "Prodigal Sorcerer")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dovin = game.findPermanent("Dovin Baan")!!
            setLoyalty(game, dovin, 3)
            val sorcerer = game.findPermanent("Prodigal Sorcerer")!!

            activate(game, dovin, index = 0, targets = listOf(sorcerer)).error shouldBe null
            game.resolveStack()
            loyalty(game, dovin) shouldBe 4
            game.state.projectedState.getPower(sorcerer) shouldBe -2

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.projectedState.getPower(sorcerer) shouldBe -2
            pingOpponent(game, sorcerer).error shouldNotBe null

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.projectedState.getPower(sorcerer) shouldBe 1
        }

        test("+1 may be activated with no target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dovin Baan")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dovin = game.findPermanent("Dovin Baan")!!
            setLoyalty(game, dovin, 3)
            activate(game, dovin, index = 0).error shouldBe null
            game.resolveStack()
            loyalty(game, dovin) shouldBe 4
        }

        test("−1 gains 2 life and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dovin Baan")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dovin = game.findPermanent("Dovin Baan")!!
            setLoyalty(game, dovin, 3)
            val hand = game.handSize(1)
            activate(game, dovin, index = 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
            game.handSize(1) shouldBe hand + 1
            loyalty(game, dovin) shouldBe 2
        }

        test("−7 emblem: the opponent untaps only two permanents, Dovin's controller untaps all") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dovin Baan")
                .withCardOnBattlefield(1, "Island", tapped = true)
                .withCardOnBattlefield(1, "Island", tapped = true)
                .withCardOnBattlefield(1, "Island", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Forest", tapped = true)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dovin = game.findPermanent("Dovin Baan")!!
            setLoyalty(game, dovin, 7)
            activate(game, dovin, index = 2).error shouldBe null
            game.resolveStack()
            game.state.entities.values.count { it.has<EmblemStaticAbilityComponent>() } shouldBe 1

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.findPermanents("Forest").count { isTapped(game, it) } shouldBe 2

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.findPermanents("Island").none { isTapped(game, it) } shouldBe true
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<EntityId> = emptyList()): ExecutionResult {
        val ability = cardRegistry.getCard("Dovin Baan")!!.script.activatedAbilities[index]
        return game.execute(ActivateAbility(
            playerId = game.player1Id, sourceId = source, abilityId = ability.id,
            targets = targets.map { ChosenTarget.Permanent(it) }))
    }

    private fun pingOpponent(game: TestGame, sorcerer: EntityId): ExecutionResult {
        val ability = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities.first()
        return game.execute(ActivateAbility(
            playerId = game.player2Id, sourceId = sorcerer, abilityId = ability.id,
            targets = listOf(ChosenTarget.Player(game.player1Id))))
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c -> c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount)) }
    }

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true
}
