package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Leonin Lightbringer (ONE) — Ward {2}; "As long as this creature is equipped, it gets +1/+1."
 */
class LeoninLightbringerScenarioTest : ScenarioTestBase() {

    init {
        context("Leonin Lightbringer — equipped-gated +1/+1") {
            test("unequipped, it is a 3/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Leonin Lightbringer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cat = game.findPermanent("Leonin Lightbringer").shouldNotBeNull()
                val projected = game.state.projectedState
                projected.getPower(cat) shouldBe 3
                projected.getToughness(cat) shouldBe 2
                projected.hasKeyword(cat, Keyword.WARD) shouldBe true
            }

            test("equipped, it is a 4/3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Leonin Lightbringer")
                    .withCardAttachedTo(1, "Whispersilk Cloak", "Leonin Lightbringer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cat = game.findPermanent("Leonin Lightbringer").shouldNotBeNull()
                val projected = game.state.projectedState
                withClue("Whispersilk Cloak changes no stats itself") {
                    projected.getPower(cat) shouldBe 4
                    projected.getToughness(cat) shouldBe 3
                }
            }

            test("an Aura is not Equipment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Leonin Lightbringer")
                    .withCardAttachedTo(1, "Pacifism", "Leonin Lightbringer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cat = game.findPermanent("Leonin Lightbringer").shouldNotBeNull()
                val projected = game.state.projectedState
                projected.getPower(cat) shouldBe 3
                projected.getToughness(cat) shouldBe 2
            }
        }
    }
}
