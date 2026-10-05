package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Launch Mishap (J22 #14) — {2}{U} Instant.
 *
 *   Counter target creature or planeswalker spell. Create a 1/1 colorless Thopter artifact
 *   creature token with flying.
 */
class LaunchMishapScenarioTest : ScenarioTestBase() {
    init {
        fun thopters(game: TestGame, playerNumber: Int) =
            game.state.getZone(if (playerNumber == 1) game.player1Id else game.player2Id, Zone.BATTLEFIELD)
                .filter { game.state.projectedState.hasSubtype(it, "Thopter") }

        test("counters a creature spell and gives the caster a 1/1 flying Thopter artifact") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Launch Mishap")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Launch Mishap", "Grizzly Bears").error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is countered") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            val made = thopters(game, 1)
            withClue("the caster gets exactly one Thopter; the countered player gets none") {
                made.size shouldBe 1
                thopters(game, 2).size shouldBe 0
            }
            val thopter = made.single()
            val projected = game.state.projectedState
            projected.isCreature(thopter) shouldBe true
            projected.hasType(thopter, "ARTIFACT") shouldBe true
            projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            projected.getPower(thopter) shouldBe 1
            projected.getToughness(thopter) shouldBe 1
            projected.getColors(thopter).isEmpty() shouldBe true
        }

        test("cannot target a noncreature, nonplaneswalker spell") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Launch Mishap")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Divination")
                .withLandsOnBattlefield(2, "Island", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Divination").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Launch Mishap", "Divination").error shouldNotBe null
            thopters(game, 1).size shouldBe 0
        }
    }
}
