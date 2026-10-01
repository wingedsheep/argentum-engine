package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Charge of the Mites (ONE #6) — {2}{W} Instant.
 *
 * Choose one —
 * • Deals damage equal to the number of creatures you control to target creature or planeswalker.
 * • Create two 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and "This token can't block."
 */
class ChargeOfTheMitesScenarioTest : ScenarioTestBase() {
    init {
        test("mode 1 deals damage equal to the number of creatures you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Charge of the Mites")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpellWithMode(1, "Charge of the Mites", 0, giant).error shouldBe null
            game.resolveStack()

            withClue("two creatures you control -> 2 damage, Hill Giant survives") {
                game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
                game.findPermanent("Hill Giant") shouldBe giant
            }
            game.isInGraveyard(1, "Charge of the Mites") shouldBe true
        }

        test("mode 1 kills when you control enough creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Charge of the Mites")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpellWithMode(1, "Charge of the Mites", 0, giant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("mode 2 creates two Phyrexian Mite tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Charge of the Mites")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithMode(1, "Charge of the Mites", 1).error shouldBe null
            game.resolveStack()

            val mites = game.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 2
            val projected = game.state.projectedState
            mites.forEach { mite ->
                projected.getController(mite) shouldBe game.player1Id
                projected.getPower(mite) shouldBe 1
                projected.getToughness(mite) shouldBe 1
                projected.hasType(mite, "ARTIFACT") shouldBe true
                projected.hasKeyword(mite, Keyword.TOXIC) shouldBe true
            }
        }
    }
}
