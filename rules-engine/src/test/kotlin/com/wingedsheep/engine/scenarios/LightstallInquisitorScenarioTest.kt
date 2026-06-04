package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lightstall Inquisitor (EOE #24).
 *
 * "{W} Creature — Angel Wizard 2/1. Vigilance.
 *  When this creature enters, each opponent exiles a card from their hand and may play
 *  that card for as long as it remains exiled. Each spell cast this way costs {1} more
 *  to cast. Each land played this way enters tapped."
 *
 * Verifies that the ETB pipeline:
 *  - moves an opponent-chosen card from that opponent's hand to their own exile,
 *  - grants a permanent may-play permission to the opponent (not to the caster),
 *  - flags the exiled card with PlayWithCostIncreaseComponent for the {1} surcharge,
 *  - and forces lands played via the permission to enter tapped.
 */
class LightstallInquisitorScenarioTest : ScenarioTestBase() {

    init {
        context("Lightstall Inquisitor ETB") {

            test("casting Lightstall Inquisitor exiles opponent-chosen card and stamps permission, cost-increase, and land-tap markers") {
                var builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Lightstall Inquisitor")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    // Two cards in opponent's hand → the SelectFromCollection step actually
                    // prompts the opponent rather than auto-picking a single legal target.
                    .withCardInHand(2, "Mountain")
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Library fuel so neither player decks on step advances.
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val mountainId = game.findCardsInHand(2, "Mountain").single()

                val cast = game.castSpell(1, "Lightstall Inquisitor")
                cast.error shouldBe null
                game.resolveStack()

                // The ETB trigger pauses on the opponent's "choose a card from your hand"
                // decision; player 2 selects the Mountain.
                withClue("ETB trigger should pause for the opponent's exile selection") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision()!!.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf(mountainId))
                game.resolveStack()

                withClue("The Mountain should be in player 2's exile") {
                    game.state.getExile(game.player2Id).contains(mountainId) shouldBe true
                }
                withClue("The Mountain should no longer be in player 2's hand") {
                    game.state.getHand(game.player2Id).contains(mountainId) shouldBe false
                }

                val mayPlay = game.state.mayPlayPermissions.firstOrNull { mountainId in it.cardIds }
                withClue("A may-play permission for the exiled card must exist") {
                    mayPlay.shouldNotBeNull()
                }
                withClue("The may-play permission must belong to the opponent (the card's owner)") {
                    mayPlay!!.controllerId shouldBe game.player2Id
                }
                withClue("The permission must persist for as long as the card remains exiled") {
                    mayPlay!!.permanent shouldBe true
                }
                withClue("The permission must force any played land tapped") {
                    mayPlay!!.landEntersTapped shouldBe true
                }

                val costIncrease = game.state.getEntity(mountainId)?.get<PlayWithCostIncreaseComponent>()
                withClue("Exiled card must carry the {1} cast surcharge for the opponent") {
                    costIncrease.shouldNotBeNull()
                }
                withClue("Cost increase must apply to the opponent (the player who may cast it)") {
                    costIncrease!!.controllerId shouldBe game.player2Id
                }
                costIncrease!!.amount shouldBe 1
            }

            test("a land played from this exile enters the battlefield tapped") {
                var builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Lightstall Inquisitor")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Mountain")
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val mountainId = game.findCardsInHand(2, "Mountain").single()

                game.castSpell(1, "Lightstall Inquisitor")
                game.resolveStack()
                game.selectCards(listOf(mountainId))
                game.resolveStack()

                // Hand the active turn over to player 2 so they can play their land.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val playResult = game.execute(PlayLand(game.player2Id, mountainId))
                playResult.error shouldBe null

                withClue("Mountain should have entered player 2's battlefield") {
                    game.state.getBattlefield(game.player2Id).contains(mountainId) shouldBe true
                }
                val mountainContainer = game.state.getEntity(mountainId)
                withClue("Land played via Lightstall Inquisitor's permission must enter tapped") {
                    (mountainContainer?.get<TappedComponent>() != null) shouldBe true
                }
                withClue("Card name preserved through the zone change") {
                    game.state.getEntity(mountainId)?.get<CardComponent>()?.name shouldBe "Mountain"
                }
            }

            test("opponent cannot cast the exiled spell when they only have base-cost mana — the +{1} surcharge is enforced at cast time") {
                // Grizzly Bears costs {1}{G}; the Lightstall surcharge raises it to {2}{G}.
                // Two Forests is exactly enough for the base cost but one short of the surcharged cost,
                // so a successful CastSpell here would prove the {1} clause is NOT being applied.
                var builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Lightstall Inquisitor")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    // Decoy card so SelectFromCollection actually prompts the opponent
                    // instead of auto-picking the single legal target.
                    .withCardInHand(2, "Plains")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val grizzlyId = game.findCardsInHand(2, "Grizzly Bears").single()

                game.castSpell(1, "Lightstall Inquisitor")
                game.resolveStack()
                game.selectCards(listOf(grizzlyId))
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val castResult = game.castSpellFromExile(2, "Grizzly Bears")
                withClue("Casting at base cost {1}{G} with only 2 Forests must fail because the surcharge raises the effective cost to {2}{G}") {
                    castResult.error shouldBe "Not enough mana to cast this spell"
                }
                withClue("The card stays in exile when the cast attempt fails") {
                    game.state.getExile(game.player2Id).contains(grizzlyId) shouldBe true
                }
            }

            test("opponent can cast the exiled spell when they have enough mana for the surcharged cost") {
                // Surcharged cost is {2}{G}; three Forests cover it exactly. Resolving onto the
                // battlefield proves the cost computation accepted the surcharge.
                var builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Lightstall Inquisitor")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    // Decoy card so SelectFromCollection actually prompts the opponent.
                    .withCardInHand(2, "Plains")
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val grizzlyId = game.findCardsInHand(2, "Grizzly Bears").single()

                game.castSpell(1, "Lightstall Inquisitor")
                game.resolveStack()
                game.selectCards(listOf(grizzlyId))
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val castResult = game.castSpellFromExile(2, "Grizzly Bears")
                withClue("Three Forests cover the surcharged {2}{G} cost — cast must succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears must resolve onto opponent's battlefield") {
                    game.state.getBattlefield(game.player2Id).contains(grizzlyId) shouldBe true
                }
            }
        }
    }
}
