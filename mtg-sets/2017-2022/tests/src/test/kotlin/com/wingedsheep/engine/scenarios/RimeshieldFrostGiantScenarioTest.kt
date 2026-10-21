package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rimeshield Frost Giant (AFR #69): Ward {3}. An opponent's removal spell is countered unless
 * they pay {3} more.
 */
class RimeshieldFrostGiantScenarioTest : ScenarioTestBase() {

    init {
        test("an opponent who can't pay ward {3} has the removal spell countered") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Rimeshield Frost Giant")
                .withCardInHand(2, "Doom Blade")
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Doom Blade", game.findPermanent("Rimeshield Frost Giant")!!).error shouldBe null
            game.resolveStack()

            withClue("ward countered Doom Blade") {
                game.isOnBattlefield("Rimeshield Frost Giant") shouldBe true
                game.isInGraveyard(2, "Doom Blade") shouldBe true
            }
        }

        test("paying ward {3} lets the removal resolve") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Rimeshield Frost Giant")
                .withCardInHand(2, "Doom Blade")
                .withLandsOnBattlefield(2, "Swamp", 5)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Doom Blade", game.findPermanent("Rimeshield Frost Giant")!!).error shouldBe null
            game.resolveStack()

            val ward = game.getPendingDecision()
            ward.shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitDecision(ManaSourcesSelectedResponse(ward.id, autoPay = true)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Rimeshield Frost Giant") shouldBe true
        }
    }
}
