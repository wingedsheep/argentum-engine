package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dawnhart Rejuvenator (MID #180) — "When this creature enters, you gain 3 life.
 * {T}: Add one mana of any color."
 */
class DawnhartRejuvenatorScenarioTest : ScenarioTestBase() {

    init {
        test("entering gains its controller 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dawnhart Rejuvenator")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Dawnhart Rejuvenator").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Dawnhart Rejuvenator") shouldBe true
            game.getLifeTotal(1) shouldBe 23
            game.getLifeTotal(2) shouldBe 20
        }

        test("its tap ability pays for an off-colour spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dawnhart Rejuvenator")
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val abilityId = cardRegistry.getCard("Dawnhart Rejuvenator")!!.activatedAbilities.first().id
            game.getLegalActions(1).any { (it.action as? ActivateAbility)?.abilityId == abilityId } shouldBe true

            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
        }
    }
}
