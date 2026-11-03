package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kumena, Tyrant of Orazca (RIX #162) —
 *   Tap another untapped Merfolk you control: Kumena can't be blocked this turn.
 *   Tap three untapped Merfolk you control: Draw a card.
 *   Tap five untapped Merfolk you control: Put a +1/+1 counter on each Merfolk you control.
 */
class KumenaTyrantOfOrazcaScenarioTest : ScenarioTestBase() {

    private val abilities = cardRegistry.getCard("Kumena, Tyrant of Orazca")!!.activatedAbilities.map { it.id }

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(merfolk: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Kumena, Tyrant of Orazca")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(merfolk) { builder.withCardOnBattlefield(1, "Merfolk Looter") }
        return builder.build()
    }

    private fun TestGame.activate(index: Int, tapped: List<EntityId>) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Kumena, Tyrant of Orazca")!!,
            abilityId = abilities[index],
            costPayment = AdditionalCostPayment(tappedPermanents = tapped),
        )
    )

    init {
        test("tapping another Merfolk makes Kumena unblockable, but Kumena can't tap itself for it") {
            val game = board(merfolk = 1)
            val kumena = game.findPermanent("Kumena, Tyrant of Orazca")!!
            val looter = game.findPermanent("Merfolk Looter")!!

            game.activate(0, listOf(kumena)).error shouldNotBe null
            game.activate(0, listOf(looter)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(kumena, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }

        test("tapping three Merfolk, Kumena included, draws a card; a non-Merfolk can't pay") {
            val game = board(merfolk = 2)
            val kumena = game.findPermanent("Kumena, Tyrant of Orazca")!!
            val looters = game.findAllPermanents("Merfolk Looter")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activate(1, listOf(looters[0], looters[1], bears)).error shouldNotBe null
            val handBefore = game.handSize(1)
            game.activate(1, listOf(kumena) + looters).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }

        test("tapping five Merfolk puts a +1/+1 counter on each Merfolk you control") {
            val game = board(merfolk = 5)
            val kumena = game.findPermanent("Kumena, Tyrant of Orazca")!!
            val looters = game.findAllPermanents("Merfolk Looter")

            game.activate(2, looters).error shouldBe null
            game.resolveStack()

            game.plusOnes(kumena) shouldBe 1
            looters.forEach { game.plusOnes(it) shouldBe 1 }
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
        }
    }
}
