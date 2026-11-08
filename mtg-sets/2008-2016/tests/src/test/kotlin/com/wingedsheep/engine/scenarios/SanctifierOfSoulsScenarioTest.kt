package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.emn.cards.SanctifierOfSouls
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sanctifier of Souls (EMN #39) — {3}{W} 2/3 Creature — Human Cleric.
 *
 *   Whenever another creature you control enters, this creature gets +1/+1 until end of turn.
 *   {2}{W}, Exile a creature card from your graveyard: Create a 1/1 white Spirit creature token
 *   with flying.
 */
class SanctifierOfSoulsScenarioTest : ScenarioTestBase() {

    init {
        test("another creature entering under your control pumps it +1/+1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sanctifier of Souls")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Grizzly Bears")
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val sanctifier = game.findPermanent("Sanctifier of Souls")!!
            game.state.projectedState.getPower(sanctifier) shouldBe 3
            game.state.projectedState.getToughness(sanctifier) shouldBe 4
        }

        test("exiling a creature card from the graveyard makes a 1/1 flying Spirit") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sanctifier of Souls")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sanctifier = game.findPermanent("Sanctifier of Souls")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = sanctifier,
                    abilityId = SanctifierOfSouls.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            val spirit = game.findPermanents("Spirit Token").single()
            game.state.projectedState.getPower(spirit) shouldBe 1
            game.state.projectedState.getToughness(spirit) shouldBe 1
            game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
        }
    }
}
