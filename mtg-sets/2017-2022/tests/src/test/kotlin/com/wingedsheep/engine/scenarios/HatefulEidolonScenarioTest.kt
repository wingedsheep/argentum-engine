package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hateful Eidolon (THB #101) — {B} Enchantment Creature — Spirit, 1/2, lifelink.
 *
 * "Whenever an enchanted creature dies, draw a card for each Aura you controlled that was attached
 *  to it."
 *
 * Pins the last-known Aura count: the Auras have been swept off the dead creature by the time the
 * trigger resolves, only the ones *you* controlled count (an opponent's Aura still makes the
 * creature "enchanted" but draws nothing), and an Aura destroyed at the same time as its creature
 * still counts (ruling).
 */
class HatefulEidolonScenarioTest : ScenarioTestBase() {

    private fun builder() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hateful Eidolon")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withLandsOnBattlefield(1, "Plains", 6)
        .apply { repeat(5) { withCardInLibrary(1, "Swamp") } }
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Hateful Eidolon") {

            test("draws one card for each Aura you controlled, ignoring an opponent's Aura") {
                val game = builder()
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears")
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                withClue("the Auras fell off before the trigger resolved") {
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                    game.isInGraveyard(2, "Pacifism") shouldBe true
                }
                withClue("two Auras you controlled -> two cards; the opponent's Pacifism adds none") {
                    game.handSize(1) shouldBe 2
                }
            }

            test("a creature enchanted only by an opponent's Aura triggers but draws nothing") {
                val game = builder()
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .build()

                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 0
            }

            test("an Aura destroyed at the same time as its creature still counts, and the Eidolon dying with it still triggers") {
                val game = builder()
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Unholy Strength", "Grizzly Bears")
                    .withCardInHand(1, "Akroma's Vengeance")
                    .build()

                game.castSpell(1, "Akroma's Vengeance").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                withClue("Akroma's Vengeance destroyed the creatures and the Auras together") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Holy Strength") shouldBe true
                    game.isInGraveyard(1, "Hateful Eidolon") shouldBe true
                }
                withClue("both Auras you controlled count from last-known information") {
                    game.handSize(1) shouldBe 2
                }
            }
        }
    }
}
