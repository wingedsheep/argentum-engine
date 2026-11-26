package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Disciple of Bolas (M13 #88) — {3}{B} Creature — Human Wizard 2/1.
 *
 *   When this creature enters, sacrifice another creature. You gain X life and draw X cards,
 *   where X is that creature's power.
 *
 * X is read from the sacrificed creature's last-known power, after it has left the battlefield.
 */
class DiscipleOfBolasScenarioTest : ScenarioTestBase() {
    init {
        test("sacrifices the chosen other creature and uses its last-known (anthem-boosted) power") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Disciple of Bolas")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Glorious Anthem")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val handBefore = game.handSize(1) - 1

            game.castSpell(1, "Disciple of Bolas").error shouldBe null
            game.resolveStack()

            withClue("two other creatures: the controller chooses which to sacrifice") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant (3/3, 4/4 with the anthem) was sacrificed; the Disciple stays") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Disciple of Bolas") shouldBe true
            }
            withClue("X = 4, the Giant's power as it last existed on the battlefield") {
                game.getLifeTotal(1) shouldBe 24
                game.handSize(1) shouldBe handBefore + 4
            }
        }

        test("with no other creature, nothing is sacrificed and X is 0") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Disciple of Bolas")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Disciple of Bolas").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Disciple of Bolas") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.handSize(1) shouldBe 0
        }
    }
}
