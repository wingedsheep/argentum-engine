package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bng.cards.EverflameEidolon
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Everflame Eidolon: "{R}: This creature gets +1/+0 until end of turn. If it's an Aura, enchanted
 * creature gets +1/+0 until end of turn instead. Enchanted creature gets +1/+1."
 * The pump lands on the Eidolon itself as a creature, and on the host when bestowed.
 */
class EverflameEidolonScenarioTest : ScenarioTestBase() {

    private val pumpId = EverflameEidolon.activatedAbilities.first().id

    init {
        test("as a creature, {R} gives the Eidolon itself +1/+0") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Everflame Eidolon")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eidolon = game.findPermanent("Everflame Eidolon")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = eidolon, abilityId = pumpId)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(eidolon) shouldBe 2
            projected.getToughness(eidolon) shouldBe 1
        }

        test("bestowed, {R} pumps the enchanted creature instead") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Everflame Eidolon")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Everflame Eidolon").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val eidolon = game.findPermanent("Everflame Eidolon")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = eidolon, abilityId = pumpId)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(eidolon) shouldBe false
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 3
        }
    }
}
