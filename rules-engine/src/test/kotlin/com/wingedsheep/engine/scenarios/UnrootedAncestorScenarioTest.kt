package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Unrooted Ancestor. */
class UnrootedAncestorScenarioTest : ScenarioTestBase() {

    private val rainveilManaAbilityId =
        cardRegistry.getCard("Rainveil Rejuvenator")!!.activatedAbilities.first().id
    private val unrootedAbilityId =
        cardRegistry.getCard("Unrooted Ancestor")!!.activatedAbilities.first().id

    init {
        context("Unrooted Ancestor") {
            test("activated ability sacrifices another creature, grants indestructible, and taps itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Unrooted Ancestor")
                    .withCardOnBattlefield(1, "Glory Seeker") // fodder
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancestor = game.findPermanent("Unrooted Ancestor")!!
                val fodder = game.findPermanent("Glory Seeker")!!

                withClue("Unrooted Ancestor is not indestructible before activation") {
                    game.state.projectedState.hasKeyword(ancestor, Keyword.INDESTRUCTIBLE) shouldBe false
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ancestor,
                        abilityId = unrootedAbilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                    )
                )
                withClue("Activating Unrooted Ancestor should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Glory Seeker should be sacrificed to the graveyard") {
                    game.findPermanents("Glory Seeker").contains(fodder) shouldBe false
                    game.findCardsInGraveyard(1, "Glory Seeker").size shouldBe 1
                }
                withClue("Unrooted Ancestor gains indestructible") {
                    game.state.projectedState.hasKeyword(ancestor, Keyword.INDESTRUCTIBLE) shouldBe true
                }
                withClue("Unrooted Ancestor is tapped by its own ability") {
                    game.state.getEntity(ancestor)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
