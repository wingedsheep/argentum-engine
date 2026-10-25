package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Monk Class {W}{U} — Enchantment — Class (AFR #228).
 *
 * Pins level 3's conditional cast permission: the upkeep-exiled card can't be cast until you've
 * cast another spell this turn, and then it can — as that second spell, it also gets level 1's
 * {1} reduction.
 */
class MonkClassScenarioTest : ScenarioTestBase() {

    init {
        context("Monk Class level 3") {

            test("the upkeep-exiled card is castable only once you've cast another spell this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Monk Class", classLevel = 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInHand(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id

                withClue("the top card was exiled at the beginning of your upkeep") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
                withClue("no spell cast yet this turn: the exiled card can't be cast") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldNotBe null
                }

                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()

                withClue("after another spell, it's castable — and as the second spell it costs only {G}") {
                    game.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
                }
                game.resolveStack()
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }
    }
}
