package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The One Ring — {4} Legendary Artifact
 *   Indestructible
 *   When The One Ring enters, if you cast it, you gain protection from everything until your next turn.
 *   At the beginning of your upkeep, you lose 1 life for each burden counter on The One Ring.
 *   {T}: Put a burden counter on The One Ring, then draw a card for each burden counter on The One Ring.
 *
 * Exercises Gap 8 (player-level protection from everything) plus the burden-counter tap/upkeep loop.
 */
class TheOneRingScenarioTest : ScenarioTestBase() {

    private val tapAbilityId by lazy {
        cardRegistry.requireCard("The One Ring").activatedAbilities[0].id
    }

    init {
        test("casting The One Ring grants the controller protection from everything") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "The One Ring")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "The One Ring").error shouldBe null
            game.resolveStack()

            // ETB ("if you cast it") granted the controller player-level protection.
            game.state.getEntity(game.player1Id)?.get<PlayerProtectionComponent>() shouldNotBe null

            // A protected player can't be targeted: opponent's Lightning Bolt can't choose player 1.
            // Pass priority so player 2 gets a window during player 1's main phase.
            game.passPriority()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldNotBe null
        }

        test("tap adds a burden counter then draws that many; upkeep loses that much life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "The One Ring")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withLifeTotal(1, 20)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ring = game.findPermanent("The One Ring")!!
            val handBefore = game.handSize(1)

            // Pass priority to player 1, who activates The One Ring's tap ability at instant speed.
            game.passPriority()
            // {T}: first activation -> 1 burden counter, draw 1.
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = ring, abilityId = tapAbilityId)
            ).error shouldBe null
            game.resolveStack()
            game.handSize(1) shouldBe handBefore + 1

            // Advance to player 1's upkeep (next turn): the "at the beginning of your upkeep"
            // trigger loses 1 life for the single burden counter.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 19
        }
    }
}
