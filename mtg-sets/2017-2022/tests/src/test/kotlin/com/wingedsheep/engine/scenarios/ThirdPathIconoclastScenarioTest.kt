package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Third Path Iconoclast (BRO #223) — whenever you cast a noncreature spell, create a 1/1
 * colorless Soldier artifact creature token. A creature spell must not trigger it.
 */
class ThirdPathIconoclastScenarioTest : ScenarioTestBase() {

    init {
        test("a noncreature spell creates a 1/1 Soldier artifact creature token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Third Path Iconoclast", summoningSickness = false)
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            val tokens = game.findPermanents("Soldier Token")
            withClue("exactly one Soldier token") { tokens.size shouldBe 1 }
            val token = tokens.single()
            val projected = game.state.projectedState
            withClue("the token is a 1/1 artifact creature") {
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
                projected.hasType(token, "ARTIFACT") shouldBe true
                projected.isCreature(token) shouldBe true
            }
        }

        test("a creature spell creates no token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Third Path Iconoclast", summoningSickness = false)
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.findPermanents("Soldier Token").size shouldBe 0
        }
    }
}
