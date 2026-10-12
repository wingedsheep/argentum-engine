package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ivy, Gleeful Spellthief — "Whenever a player casts a spell that targets only a single creature
 * other than Ivy, you may copy that spell. The copy targets Ivy."
 *
 * Exercises `SpellCastPredicate.TargetsMatching(…, only = true)` and
 * `CopyForEachOtherPossibleTargetEffect` with a candidate filter read relative to Ivy.
 */
class IvyGleefulSpellthiefScenarioTest : ScenarioTestBase() {

    init {
        test("copying your own pump spell gives Ivy the copy") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ivy, Gleeful Spellthief")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val ivy = game.findPermanent("Ivy, Gleeful Spellthief")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.getPower(ivy) shouldBe 5
        }

        test("an opponent's spell is copied by Ivy's controller, aimed at Ivy") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Ivy, Gleeful Spellthief")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val ivy = game.findPermanent("Ivy, Gleeful Spellthief")!!
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            game.state.projectedState.getPower(ivy) shouldBe 5
            game.state.projectedState.getPower(bears) shouldBe 5
        }

        test("declining makes no copy") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ivy, Gleeful Spellthief")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ivy = game.findPermanent("Ivy, Gleeful Spellthief")!!
            game.castSpell(1, "Giant Growth", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.state.projectedState.getPower(ivy) shouldBe 2
        }

        test("a spell that targets Ivy herself does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ivy, Gleeful Spellthief")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ivy = game.findPermanent("Ivy, Gleeful Spellthief")!!
            game.castSpell(1, "Giant Growth", ivy).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.state.projectedState.getPower(ivy) shouldBe 5
        }

        test("a spell that targets a player does not trigger") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ivy, Gleeful Spellthief")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(2) shouldBe 17
        }

        test("no copy when Ivy isn't a legal target for the spell") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Ivy, Gleeful Spellthief")
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .withCardInHand(1, "Vengeance")
                .withLandsOnBattlefield(1, "Plains", 4)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Vengeance", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.answerYesNo(true)
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Ivy, Gleeful Spellthief") shouldBe true
        }
    }
}
