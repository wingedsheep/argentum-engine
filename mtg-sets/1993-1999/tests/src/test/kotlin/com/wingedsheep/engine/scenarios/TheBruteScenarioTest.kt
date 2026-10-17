package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.leg.cards.TheBrute
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * The Brute (LEG): "Enchant creature / Enchanted creature gets +1/+0. / {R}{R}{R}: Regenerate
 * enchanted creature."
 */
class TheBruteScenarioTest : ScenarioTestBase() {
    private val abilityId = TheBrute.activatedAbilities.single().id

    init {
        test("the enchanted creature gets +1/+0 and {R}{R}{R} regenerates it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "The Brute", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 2

            val brute = game.findPermanent("The Brute")!!
            game.execute(ActivateAbility(game.player1Id, brute, abilityId)).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
