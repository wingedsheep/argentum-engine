package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.emn.cards.StromkirkCondemned
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stromkirk Condemned (EMN #106) — {B}{B} 2/2 Creature — Vampire Horror.
 *
 *   Discard a card: Vampires you control get +1/+1 until end of turn. Activate only once each turn.
 */
class StromkirkCondemnedScenarioTest : ScenarioTestBase() {

    init {
        test("discarding a card pumps only the Vampires you control, once per turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Stromkirk Condemned")
                .withCardOnBattlefield(1, "Vampire Interloper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Bloodthirsty Aerialist")
                .withCardsInHand(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val condemned = game.findPermanent("Stromkirk Condemned")!!
            val interloper = game.findPermanent("Vampire Interloper")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val opposingVampire = game.findPermanent("Bloodthirsty Aerialist")!!
            val (first, second) = game.findCardsInHand(1, "Swamp")
            val abilityId = StromkirkCondemned.activatedAbilities.first().id

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = condemned,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(first)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.GRAVEYARD).contains(first) shouldBe true
            val projected = game.state.projectedState
            withClue("Vampires you control get +1/+1") {
                projected.getPower(condemned) shouldBe 3
                projected.getToughness(condemned) shouldBe 3
                projected.getPower(interloper) shouldBe 3
                projected.getToughness(interloper) shouldBe 2
            }
            withClue("non-Vampires and opposing Vampires are untouched") {
                projected.getPower(bears) shouldBe 2
                projected.getPower(opposingVampire) shouldBe 2
            }

            val again = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = condemned,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(second)),
                )
            )
            withClue("activate only once each turn") { again.error shouldNotBe null }
        }
    }
}
