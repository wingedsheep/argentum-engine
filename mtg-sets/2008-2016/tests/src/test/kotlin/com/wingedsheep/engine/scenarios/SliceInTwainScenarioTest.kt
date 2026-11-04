package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class SliceInTwainScenarioTest : ScenarioTestBase() {
    init {
        for (permanent in listOf("Ornithopter", "Asceticism")) {
            test("destroys $permanent and draws a card") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Slice in Twain")
                    .withCardOnBattlefield(2, permanent)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Slice in Twain", game.findPermanent(permanent)!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, permanent) shouldBe true
                game.isInHand(1, "Island") shouldBe true
            }
        }

        test("a legal indestructible target survives but still allows the draw") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Slice in Twain")
                .withCardOnBattlefield(2, "Darksteel Ingot")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ingot = game.findPermanent("Darksteel Ingot")!!
            game.castSpell(1, "Slice in Twain", ingot).error shouldBe null
            game.resolveStack()

            game.findPermanent("Darksteel Ingot") shouldBe ingot
            game.isInHand(1, "Island") shouldBe true
        }

        test("an illegal target on resolution prevents the draw") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Slice in Twain")
                .withCardInHand(2, "Unsummon")
                .withCardOnBattlefield(2, "Ornithopter")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val thopter = game.findPermanent("Ornithopter")!!
            game.castSpell(1, "Slice in Twain", thopter).error shouldBe null
            game.passPriority()
            game.castSpell(2, "Unsummon", thopter).error shouldBe null
            game.resolveStack()

            game.isInHand(2, "Ornithopter") shouldBe true
            game.isInHand(1, "Swamp") shouldBe false
            game.isInGraveyard(1, "Slice in Twain") shouldBe true
        }
    }
}
