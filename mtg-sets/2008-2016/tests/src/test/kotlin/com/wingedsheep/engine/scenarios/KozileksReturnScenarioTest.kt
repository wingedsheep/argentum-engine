package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kozilek's Return (OGW #98) — {2}{R} Instant, devoid.
 *
 *   Kozilek's Return deals 2 damage to each creature.
 *   Whenever you cast an Eldrazi creature spell with mana value 7 or greater, you may exile this
 *   card from your graveyard. If you do, this card deals 5 damage to each creature.
 *
 * Sire of Seven Deaths ({7} Eldrazi) sits exactly on the threshold; Kozilek's Channeler ({5}
 * Eldrazi) is under it, and Duskdale Wurm ({5}{G}{G}) is a 7-drop that isn't an Eldrazi.
 */
class KozileksReturnScenarioTest : ScenarioTestBase() {

    /** Kozilek's Return in P1's graveyard, a creature on each side, P1 holding [spell]. */
    private fun graveyardBoard(spell: String, forests: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInGraveyard(1, "Kozilek's Return")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(1, spell)
        .withLandsOnBattlefield(1, "Forest", forests)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast, it deals 2 damage to each creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Kozilek's Return")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Kozilek's Return").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
            game.isInGraveyard(1, "Kozilek's Return") shouldBe true
        }

        test("casting a mana value 7 Eldrazi lets you exile it from the graveyard to deal 5 to each creature") {
            val game = graveyardBoard("Sire of Seven Deaths", forests = 7)

            game.castSpell(1, "Sire of Seven Deaths").error shouldBe null
            // The Sire plus the graveyard trigger above it.
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Kozilek's Return") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            // The trigger resolved first: the Sire wasn't on the battlefield to be dealt damage.
            val sire = game.findPermanent("Sire of Seven Deaths")!!
            (game.state.getEntity(sire)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }

        test("declining the exile deals no damage and leaves it in the graveyard") {
            val game = graveyardBoard("Sire of Seven Deaths", forests = 7)

            game.castSpell(1, "Sire of Seven Deaths").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Kozilek's Return") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Sire of Seven Deaths") shouldBe true
        }

        test("an Eldrazi creature spell with mana value under 7 doesn't trigger it") {
            val game = graveyardBoard("Kozilek's Channeler", forests = 5)

            game.castSpell(1, "Kozilek's Channeler").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.isOnBattlefield("Kozilek's Channeler") shouldBe true
            game.isInGraveyard(1, "Kozilek's Return") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("a mana value 7 creature spell that isn't an Eldrazi doesn't trigger it") {
            val game = graveyardBoard("Duskdale Wurm", forests = 7)

            game.castSpell(1, "Duskdale Wurm").error shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.isOnBattlefield("Duskdale Wurm") shouldBe true
            game.isInGraveyard(1, "Kozilek's Return") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
