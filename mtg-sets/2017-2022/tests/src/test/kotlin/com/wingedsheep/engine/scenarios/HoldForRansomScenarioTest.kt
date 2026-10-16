package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hold for Ransom — the enchanted creature's controller activates the granted "{7}: Hold for
 * Ransom's controller sacrifices it and draws a card", but the Aura's controller is the one who
 * sacrifices it and draws ([com.wingedsheep.sdk.scripting.references.Player.ControllerOfGrantingSource]).
 */
class HoldForRansomScenarioTest : ScenarioTestBase() {

    private val ransomAbilityId by lazy {
        cardRegistry.requireCard("Hold for Ransom").script.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id
    }

    /** Player 1's Hold for Ransom on player 2's Grizzly Bears; player 2 holds seven Plains. */
    private fun ransomed(activePlayer: Int) = scenario()
        .withPlayers()
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardAttachedTo(1, "Hold for Ransom", "Grizzly Bears")
        .withLandsOnBattlefield(2, "Plains", 7)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardInHand(1, "Disenchant")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the creature's controller pays; the Aura's controller sacrifices it and draws") {
            val game = ransomed(activePlayer = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            val p1Hand = game.handSize(1)
            val p2Hand = game.handSize(2)

            game.execute(ActivateAbility(game.player2Id, bears, ransomAbilityId)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Hold for Ransom") shouldBe null
            game.isInGraveyard(1, "Hold for Ransom") shouldBe true
            game.findPermanent("Grizzly Bears") shouldNotBe null
            game.handSize(1) shouldBe p1Hand + 1
            game.handSize(2) shouldBe p2Hand
        }

        test("the Aura's controller can't activate the ability on the opponent's creature") {
            val game = ransomed(activePlayer = 1)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(ActivateAbility(game.player1Id, bears, ransomAbilityId)).error shouldNotBe null
            game.findPermanent("Hold for Ransom") shouldNotBe null
        }

        test("activate only as a sorcery: not on the Aura controller's turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Hold for Ransom", "Grizzly Bears")
                .withLandsOnBattlefield(2, "Plains", 7)
                .withActivePlayer(1)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(ActivateAbility(game.player2Id, bears, ransomAbilityId)).error shouldNotBe null
        }

        test("if the Aura leaves in response, its last controller still draws") {
            val game = ransomed(activePlayer = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            val aura = game.findPermanent("Hold for Ransom")!!

            game.execute(ActivateAbility(game.player2Id, bears, ransomAbilityId)).error shouldBe null
            game.passPriority()
            game.castSpell(1, "Disenchant", aura).error shouldBe null
            val p1Hand = game.handSize(1)
            game.resolveStack()

            game.isInGraveyard(1, "Hold for Ransom") shouldBe true
            game.handSize(1) shouldBe p1Hand + 1
        }

        test("enchanted creature can't attack or block") {
            val game = ransomed(activePlayer = 2)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.cantAttack(bears) shouldBe true
            game.state.projectedState.cantBlock(bears) shouldBe true
        }
    }
}
