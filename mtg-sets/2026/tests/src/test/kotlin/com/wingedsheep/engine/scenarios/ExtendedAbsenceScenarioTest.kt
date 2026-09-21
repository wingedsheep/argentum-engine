package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class ExtendedAbsenceScenarioTest : ScenarioTestBase() {
    init {
        for (victim in listOf("Grizzly Bears", "Jace Beleren")) {
            test("exiles $victim and damages the opponent while gaining life") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Extended Absence")
                    .withCardOnBattlefield(2, victim)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.castSpell(1, "Extended Absence", game.findPermanent(victim)!!).error shouldBe null
                game.resolveStack()
                game.isInExile(2, victim) shouldBe true
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 19
            }
        }
        test("an illegal target stops the damage and life gain too") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Extended Absence")
                .withCardInHand(2, "Unsummon")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(2, "Island", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Extended Absence", bear).error shouldBe null
            game.passPriority()
            game.castSpell(2, "Unsummon", bear).error shouldBe null
            game.resolveStack()
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
