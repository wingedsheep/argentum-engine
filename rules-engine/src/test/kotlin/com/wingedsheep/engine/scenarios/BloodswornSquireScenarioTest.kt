package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bloodsworn Squire // Bloodsworn Knight (VOW #97).
 *
 *   Front — Bloodsworn Squire (3/3) — {1}{B}, Discard a card: gains indestructible until end of
 *           turn, tap it, then if there are four or more creature cards in your graveyard, transform.
 *   Back  — Bloodsworn Knight (P/T = creature cards in your graveyard) — same ability without the
 *           transform clause.
 *
 * Exercises the composite {1}{B} + discard cost, the indestructible-then-tap effect, the
 * intervening-if transform gated on four creature cards in the graveyard (and that it does NOT
 * transform below four), and the back's characteristic-defining P/T.
 */
class BloodswornSquireScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodsworn Squire") {

            test("with four creature cards in the graveyard, the ability transforms it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodsworn Squire", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(1, "Forest") // fodder to discard
                    // Four creature cards already in the graveyard satisfies the transform gate.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val squire = game.findPermanent("Bloodsworn Squire")!!
                val fodder = game.findCardsInHand(1, "Forest").first()
                val abilityId = cardRegistry.getCard("Bloodsworn Squire")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = squire,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("indestructible was granted") {
                    game.state.projectedState.hasKeyword(squire, Keyword.INDESTRUCTIBLE) shouldBe true
                }
                withClue("the creature was tapped") {
                    game.state.getEntity(squire)!!.has<TappedComponent>() shouldBe true
                }
                withClue("four creature cards in the graveyard → transformed to Bloodsworn Knight") {
                    game.state.getEntity(squire)!!.get<CardComponent>()!!.name shouldBe "Bloodsworn Knight"
                }
                withClue("the Knight's P/T equals creature cards in the graveyard (the discarded Forest is not a creature; 4 remain)") {
                    game.state.projectedState.getPower(squire) shouldBe 4
                    game.state.projectedState.getToughness(squire) shouldBe 4
                }
            }

            test("with fewer than four creature cards in the graveyard, it does not transform") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodsworn Squire", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(1, "Forest")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears") // only three
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val squire = game.findPermanent("Bloodsworn Squire")!!
                val fodder = game.findCardsInHand(1, "Forest").first()
                val abilityId = cardRegistry.getCard("Bloodsworn Squire")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = squire,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(fodder)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("indestructible and tap still happen") {
                    game.state.projectedState.hasKeyword(squire, Keyword.INDESTRUCTIBLE) shouldBe true
                    game.state.getEntity(squire)!!.has<TappedComponent>() shouldBe true
                }
                withClue("fewer than four creature cards → stays Bloodsworn Squire") {
                    game.state.getEntity(squire)!!.get<CardComponent>()!!.name shouldBe "Bloodsworn Squire"
                }
            }
        }
    }
}
