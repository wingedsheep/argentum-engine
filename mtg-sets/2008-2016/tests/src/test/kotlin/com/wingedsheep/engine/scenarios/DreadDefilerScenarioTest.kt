package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ogw.cards.DreadDefiler
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dread Defiler (OGW #68) — {6}{B} 6/8 Creature — Eldrazi.
 *
 *   Devoid. {3}{C}, Exile a creature card from your graveyard: Target opponent loses life equal
 *   to the exiled card's power.
 *
 * Proves the life loss reads the power of the card *this activation's cost* exiled — two creature
 * cards of different power produce different losses — not a constant.
 */
class DreadDefilerScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = DreadDefiler.activatedAbilities.single().id

        for ((fuelName, power) in listOf("Centaur Courser" to 3, "Force of Nature" to 5)) {
            test("exiling $fuelName (power $power) makes the target opponent lose $power life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dread Defiler")
                    .withLandsOnBattlefield(1, "Wastes", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, fuelName)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val defiler = game.findPermanent("Dread Defiler")!!
                val fuel = game.findCardsInGraveyard(1, fuelName).single()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = defiler,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(exiledCards = listOf(fuel)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInExile(1, fuelName) shouldBe true
                game.getLifeTotal(2) shouldBe 20 - power
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
