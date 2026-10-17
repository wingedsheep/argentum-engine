package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Majestic Metamorphosis (SNC #48).
 *
 * {2}{U} Instant — "Until end of turn, target artifact or creature becomes a 4/4 Angel artifact
 * creature and gains flying. Draw a card."
 *
 * "Becomes a … artifact creature" *sets* the card types (CR 205.1a), so an enchantment creature
 * stops being an enchantment, and the creature types are replaced by Angel. Abilities and colors
 * are kept (the card's rulings).
 */
class MajesticMetamorphosisScenarioTest : ScenarioTestBase() {

    init {
        test("an enchantment creature becomes a 4/4 flying Angel artifact creature, and you draw") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Majestic Metamorphosis")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Eidolon of Rhetoric")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eidolon = game.findPermanent("Eidolon of Rhetoric")!!
            val handBefore = game.handSize(1)

            game.castSpell(1, "Majestic Metamorphosis", targetId = eidolon).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("It is a 4/4 flying artifact creature") {
                projected.isCreature(eidolon) shouldBe true
                projected.hasType(eidolon, "ARTIFACT") shouldBe true
                projected.getPower(eidolon) shouldBe 4
                projected.getToughness(eidolon) shouldBe 4
                projected.hasKeyword(eidolon, Keyword.FLYING) shouldBe true
            }
            withClue("Its card types are set: it is no longer an enchantment") {
                projected.hasType(eidolon, "ENCHANTMENT") shouldBe false
            }
            withClue("Its creature types are replaced by Angel") {
                projected.hasSubtype(eidolon, "Angel") shouldBe true
                projected.hasSubtype(eidolon, "Spirit") shouldBe false
            }
            withClue("The caster draws a card (spell left the hand, one card drawn)") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("a noncreature artifact becomes a 4/4 Angel artifact creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Majestic Metamorphosis")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(1, "Angel's Tomb")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val tomb = game.findPermanent("Angel's Tomb")!!
            game.state.projectedState.isCreature(tomb) shouldBe false

            game.castSpell(1, "Majestic Metamorphosis", targetId = tomb).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(tomb) shouldBe true
            projected.hasType(tomb, "ARTIFACT") shouldBe true
            projected.getPower(tomb) shouldBe 4
            projected.getToughness(tomb) shouldBe 4
            projected.hasSubtype(tomb, "Angel") shouldBe true
        }
    }
}
