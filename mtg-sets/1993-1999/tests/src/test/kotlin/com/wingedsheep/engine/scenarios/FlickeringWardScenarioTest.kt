package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flickering Ward (TMP) — {W} Enchantment — Aura.
 *
 *   Enchant creature
 *   As this Aura enters, choose a color.
 *   Enchanted creature has protection from the chosen color. This effect doesn't remove this Aura.
 *   {W}: Return this Aura to its owner's hand.
 *
 * With white chosen, the protection removes the other white Aura on the creature but not Flickering
 * Ward itself, and leaves an Aura of another color alone.
 */
class FlickeringWardScenarioTest : ScenarioTestBase() {

    init {
        test("the chosen-color protection removes other Auras of that color, not Flickering Ward") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")   // white
                .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears") // black
                .withCardInHand(1, "Flickering Ward")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castSpell(1, "Flickering Ward", bears)
            withClue("cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseColorDecision>()
            game.submitDecision(ColorChosenResponse(decision.id, Color.WHITE)).error shouldBe null
            game.resolveStack()

            withClue("Bears has protection from white") {
                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_WHITE") shouldBe true
            }
            withClue("Flickering Ward's own protection doesn't remove it") {
                game.isOnBattlefield("Flickering Ward") shouldBe true
            }
            withClue("Holy Strength (white) is removed (CR 704.5m)") {
                game.isInGraveyard(1, "Holy Strength") shouldBe true
            }
            withClue("Unholy Strength (black) is untouched") {
                game.isOnBattlefield("Unholy Strength") shouldBe true
            }
        }
    }
}
