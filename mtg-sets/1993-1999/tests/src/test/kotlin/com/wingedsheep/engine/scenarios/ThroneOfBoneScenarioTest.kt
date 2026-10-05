package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class ThroneOfBoneScenarioTest : ScenarioTestBase() {
    init {
        for (caster in listOf(1, 2)) {
            test("paying for player $caster's matching spell gains life for the artifact controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Throne of Bone")
                    .withCardInHand(caster, "Scathe Zombies")
                    .withLandsOnBattlefield(caster, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(caster)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(caster, "Scathe Zombies").error shouldBe null
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.getPendingDecision()!!.playerId shouldBe game.player1Id
                game.answerYesNo(true).error shouldBe null
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 20
                game.isOnBattlefield("Scathe Zombies") shouldBe true
            }
        }

        test("declining the optional payment gains no life and still resolves the spell") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Throne of Bone")
                .withCardInHand(1, "Scathe Zombies")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Scathe Zombies").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.isOnBattlefield("Scathe Zombies") shouldBe true
        }

        test("a colorless spell does not offer a payment") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Throne of Bone")
                .withCardInHand(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(1) shouldBe 20
            game.isOnBattlefield("Ornithopter") shouldBe true
        }
    }
}
