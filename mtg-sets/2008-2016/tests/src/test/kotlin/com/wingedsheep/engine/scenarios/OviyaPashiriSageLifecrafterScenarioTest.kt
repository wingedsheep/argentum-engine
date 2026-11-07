package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Oviya Pashiri, Sage Lifecrafter's Construct is X/X where X is the number of creatures you control,
 * locked in as the ability resolves: the new token doesn't count itself, and later board changes
 * don't resize it (2016-09-20 rulings).
 */
class OviyaPashiriSageLifecrafterScenarioTest : ScenarioTestBase() {
    init {
        test("Construct counts creatures you control at resolution, excluding itself, and stays that size") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Oviya Pashiri, Sage Lifecrafter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val oviya = game.findPermanent("Oviya Pashiri, Sage Lifecrafter")!!
            val constructAbility = cardRegistry.getCard("Oviya Pashiri, Sage Lifecrafter")!!
                .script.activatedAbilities[1]
            game.execute(ActivateAbility(game.player1Id, oviya, constructAbility.id)).error shouldBe null
            game.resolveStack()

            val construct = game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                .single { game.state.projectedState.hasSubtype(it, "Construct") }
            // Oviya + two Bears = 3; the opponent's Bear and the Construct itself don't count.
            game.state.projectedState.getPower(construct) shouldBe 3
            game.state.projectedState.getToughness(construct) shouldBe 3
            game.state.projectedState.getColors(construct) shouldBe emptySet()
            game.state.projectedState.isCreature(construct) shouldBe true

            // A later Servo grows the creature count, not the Construct. Walk to P1's next main
            // phase (own end step -> P2's main -> P2's end step -> P1's main) so Oviya untaps.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            val servoAbility = cardRegistry.getCard("Oviya Pashiri, Sage Lifecrafter")!!
                .script.activatedAbilities[0]
            game.execute(ActivateAbility(game.player1Id, oviya, servoAbility.id)).error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                .count { game.state.projectedState.hasSubtype(it, "Servo") } shouldBe 1
            game.state.projectedState.getPower(construct) shouldBe 3
            game.state.projectedState.getToughness(construct) shouldBe 3
        }
    }
}
