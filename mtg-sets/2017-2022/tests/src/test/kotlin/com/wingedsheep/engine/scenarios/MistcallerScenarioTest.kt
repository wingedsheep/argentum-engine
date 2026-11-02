package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mistcaller (M19 #62).
 *
 * The sacrifice ability grants a floating, turn-long Containment Priest replacement anchored to a
 * source that is already in the graveyard: a reanimated nontoken creature is exiled instead, while
 * a cast creature still enters.
 */
class MistcallerScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.sacrificeMistcaller() {
            val mistcaller = findPermanent("Mistcaller")!!
            val abilityId = cardRegistry.getCard("Mistcaller")!!.activatedAbilities.first().id
            execute(ActivateAbility(playerId = player1Id, sourceId = mistcaller, abilityId = abilityId))
                .error shouldBe null
            isOnBattlefield("Mistcaller") shouldBe false
            resolveStack()
        }

        test("after sacrificing Mistcaller, a reanimated creature is exiled instead") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mistcaller")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Zombify")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.sacrificeMistcaller()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInExile(1, "Grizzly Bears") shouldBe true
        }

        test("a creature spell cast after the sacrifice still enters") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Mistcaller")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.sacrificeMistcaller()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
