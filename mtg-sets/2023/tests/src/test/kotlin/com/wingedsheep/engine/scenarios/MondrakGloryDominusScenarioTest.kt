package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.MondrakGloryDominus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mondrak, Glory Dominus (ONE #23) — {2}{W}{W} 4/4 Legendary Creature — Phyrexian Horror.
 *
 *   If one or more tokens would be created under your control, twice that many of those tokens are
 *   created instead.
 *   {1}{W/P}{W/P}, Sacrifice two other artifacts and/or creatures: Put an indestructible counter on
 *   Mondrak.
 */
class MondrakGloryDominusScenarioTest : ScenarioTestBase() {

    init {
        test("tokens created under your control are doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mondrak, Glory Dominus")
                .withCardInHand(1, "Basilica Shepherd")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Basilica Shepherd").error shouldBe null
            game.resolveStack()

            val mites = game.findAllPermanents("Phyrexian Mite")
            withClue("two Mites doubled to four") { mites shouldHaveSize 4 }
            mites.forEach { game.state.projectedState.getController(it) shouldBe game.player1Id }
        }

        test("tokens created under an opponent's control are not doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mondrak, Glory Dominus")
                .withCardInHand(2, "Basilica Shepherd")
                .withLandsOnBattlefield(2, "Plains", 5)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Basilica Shepherd").error shouldBe null
            game.resolveStack()

            val mites = game.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 2
            mites.forEach { game.state.projectedState.getController(it) shouldBe game.player2Id }
        }

        test("sacrificing two other artifacts and/or creatures puts an indestructible counter on Mondrak") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mondrak, Glory Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Millstone")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mondrak = game.findPermanent("Mondrak, Glory Dominus")!!
            val sacrificed = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Millstone")!!)
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = mondrak,
                    abilityId = MondrakGloryDominus.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = sacrificed)
                )
            )
            withClue("activation: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(mondrak)?.get<CountersComponent>()?.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1
            game.state.projectedState.hasKeyword(mondrak, Keyword.INDESTRUCTIBLE) shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Millstone") shouldBe true
        }

        test("Mondrak cannot be one of the two sacrificed permanents") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mondrak, Glory Dominus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mondrak = game.findPermanent("Mondrak, Glory Dominus")!!
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = mondrak,
                    abilityId = MondrakGloryDominus.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(
                        sacrificedPermanents = listOf(mondrak, game.findPermanent("Grizzly Bears")!!)
                    )
                )
            )
            result.error shouldNotBe null
            game.isOnBattlefield("Mondrak, Glory Dominus") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
