package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ringwraiths — "When this creature enters, target creature an opponent controls gets -3/-3 until
 * end of turn. If that creature is legendary, its controller loses 3 life." Exercises the
 * legendary-conditional rider (ConditionalEffect + TargetMatchesFilter(legendary) + controller life
 * loss). The graveyard-return half is covered by the engine-landed graveyard-trigger framework.
 */
class RingwraithsScenarioTest : ScenarioTestBase() {

    private fun castRingwraithsTargeting(targetName: String) = scenario()
        .withPlayers()
        .withCardInHand(1, "Ringwraiths")
        .withLandsOnBattlefield(1, "Swamp", 6)
        .withCardOnBattlefield(2, targetName)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("legendary target: -3/-3 and its controller loses 3 life") {
            val game = castRingwraithsTargeting("Bill the Pony") // 1/4 legendary → survives -3/-3
            val bill = game.findPermanent("Bill the Pony")!!

            game.castSpell(1, "Ringwraiths").error shouldBe null
            game.resolveStack() // enters → ETB trigger asks for a target
            game.selectTargets(listOf(bill)).error shouldBe null
            game.resolveStack()

            // Bill survives (toughness 4 -> 1) and player 2 loses 3 life for it being legendary.
            game.isOnBattlefield("Bill the Pony") shouldBe true
            game.state.projectedState.getToughness(bill) shouldBe 1
            game.getLifeTotal(2) shouldBe 17
        }

        test("non-legendary target: -3/-3 only, no life loss") {
            val game = castRingwraithsTargeting("Grizzly Bears") // 2/2 non-legendary → dies
            val bear = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Ringwraiths").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bear)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false // -3/-3 is lethal
            game.getLifeTotal(2) shouldBe 20 // not legendary → no life loss
        }
    }
}
