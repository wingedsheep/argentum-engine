package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Green Dragon — {4}{G}{G} Creature — Dragon 4/4 (AFR #186).
 *
 * "Flying. Poison Breath — When this creature enters, until end of turn, whenever a creature an
 * opponent controls is dealt damage, destroy it."
 */
class GreenDragonScenarioTest : ScenarioTestBase() {

    private fun board(phase: Phase = Phase.PRECOMBAT_MAIN, step: Step = Step.PRECOMBAT_MAIN) = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Green Dragon")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Force of Nature")
        .withActivePlayer(1)
        .inPhase(phase, step)
        .build()

    private fun TestGame.castDragon() {
        castSpell(1, "Green Dragon").error shouldBe null
        resolveStack()
        withClue("Poison Breath resolved") { state.stack.size shouldBe 0 }
    }

    init {
        test("a creature an opponent controls that is dealt damage is destroyed") {
            val game = board()
            game.castDragon()
            val force = game.findPermanent("Force of Nature")!!

            game.castSpell(1, "Lightning Bolt", targetId = force).error shouldBe null
            game.resolveStack()

            withClue("3 damage doesn't kill the 5/5 — Poison Breath does") {
                game.isOnBattlefield("Force of Nature") shouldBe false
                game.isInGraveyard(2, "Force of Nature") shouldBe true
            }
        }

        test("your own creatures being dealt damage are left alone") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Green Dragon")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Force of Nature")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castDragon()
            val force = game.findPermanent("Force of Nature")!!

            game.castSpell(1, "Lightning Bolt", targetId = force).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Force of Nature") shouldBe true
        }

        test("it keeps working after Green Dragon leaves the battlefield") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Green Dragon")
                .withCardInHand(1, "Doom Blade")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Force of Nature")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castDragon()
            game.castSpell(1, "Doom Blade", targetId = game.findPermanent("Green Dragon")!!).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Green Dragon") shouldBe false

            game.castSpell(1, "Lightning Bolt", targetId = game.findPermanent("Force of Nature")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Force of Nature") shouldBe true
        }

        test("a blocker dealt combat damage is destroyed") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Green Dragon")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Force of Nature", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castDragon()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Force of Nature" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            withClue("Grizzly Bears' 2 damage triggered Poison Breath on the 5/5 blocker") {
                game.isInGraveyard(2, "Force of Nature") shouldBe true
            }
        }
    }
}
