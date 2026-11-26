package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Hellion Crucible (M13 #226) — Land.
 *
 *   {T}: Add {C}.
 *   {1}{R}, {T}: Put a pressure counter on this land.
 *   {1}{R}, {T}, Remove two pressure counters from this land and sacrifice it: Create a 4/4 red
 *   Hellion creature token with haste.
 */
class HellionCrucibleScenarioTest : ScenarioTestBase() {

    private val abilities = cardRegistry.getCard("Hellion Crucible")!!.activatedAbilities
    private val chargeAbilityId = abilities[1].id
    private val hellionAbilityId = abilities[2].id

    private fun TestGame.seedPressure(crucible: EntityId, amount: Int) {
        state = state.updateEntity(crucible) { it.with(CountersComponent().withAdded(CounterType.PRESSURE, amount)) }
    }

    private fun TestGame.pressureOn(crucible: EntityId): Int =
        state.getEntity(crucible)?.get<CountersComponent>()?.getCount(CounterType.PRESSURE) ?: 0

    private fun TestGame.canActivate(crucible: EntityId, abilityId: AbilityId): Boolean =
        getLegalActions(1).any {
            val a = it.action
            it.isAffordable && a is ActivateAbility && a.sourceId == crucible && a.abilityId == abilityId
        }

    private fun newGame(): TestGame {
        var builder = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Hellion Crucible")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) { builder = builder.withCardInLibrary(1, "Mountain").withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        context("Hellion Crucible") {

            test("{1}{R}, {T} puts a pressure counter on it") {
                val game = newGame()
                val crucible = game.findPermanent("Hellion Crucible")!!

                val result = game.execute(ActivateAbility(playerId = game.player1Id, sourceId = crucible, abilityId = chargeAbilityId))
                withClue("activation: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.pressureOn(crucible) shouldBe 1
            }

            test("the Hellion ability needs two pressure counters") {
                val game = newGame()
                val crucible = game.findPermanent("Hellion Crucible")!!
                game.seedPressure(crucible, 1)

                game.canActivate(crucible, hellionAbilityId) shouldBe false
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = crucible, abilityId = hellionAbilityId)
                )
                withClue("one counter can't pay the cost") { (result.error != null) shouldBe true }
                game.pressureOn(crucible) shouldBe 1
                game.findPermanents("Hellion Token").shouldBeEmpty()
            }

            test("removing two pressure counters and sacrificing it makes a 4/4 red Hellion with haste") {
                val game = newGame()
                val crucible = game.findPermanent("Hellion Crucible")!!
                game.seedPressure(crucible, 2)

                val result = game.execute(ActivateAbility(playerId = game.player1Id, sourceId = crucible, abilityId = hellionAbilityId))
                withClue("activation: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("sacrificed as a cost") { game.isInGraveyard(1, "Hellion Crucible") shouldBe true }
                val hellions = game.findPermanents("Hellion Token")
                hellions shouldHaveSize 1
                val hellion = hellions.single()
                val projected = game.state.projectedState
                projected.getPower(hellion) shouldBe 4
                projected.getToughness(hellion) shouldBe 4
                projected.getColors(hellion) shouldBe setOf("RED")
                projected.hasSubtype(hellion, "Hellion") shouldBe true
                projected.hasKeyword(hellion, Keyword.HASTE) shouldBe true
            }
        }
    }
}
