package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Skyhunter Cub (MRD) — "As long as this creature is equipped, it gets +1/+1 and has flying."
 *
 * Both halves hang off one `IsEquipped` gate but land in different layers (keyword in 6, stats in
 * 7c), so they're two conditional statics. These tests pin that the gate reads *this* creature's
 * attachments — not "an Equipment exists" and not "something is attached to anything".
 */
class SkyhunterCubScenarioTest : ScenarioTestBase() {

    init {
        context("Skyhunter Cub — equipped-gated flying and +1/+1") {
            test("unequipped, it is a plain 2/2 without flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skyhunter Cub")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cub = game.findPermanent("Skyhunter Cub").shouldNotBeNull()
                val projected = game.state.projectedState

                withClue("no Equipment attached, so neither static applies") {
                    projected.getPower(cub) shouldBe 2
                    projected.getToughness(cub) shouldBe 2
                    projected.hasKeyword(cub, Keyword.FLYING) shouldBe false
                }
            }

            test("equipped, it is a 3/3 flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skyhunter Cub")
                    .withCardAttachedTo(1, "Whispersilk Cloak", "Skyhunter Cub")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cub = game.findPermanent("Skyhunter Cub").shouldNotBeNull()
                val projected = game.state.projectedState

                withClue("Whispersilk Cloak changes no stats itself, so 3/3 is the Cub's own bonus") {
                    projected.getPower(cub) shouldBe 3
                    projected.getToughness(cub) shouldBe 3
                    projected.hasKeyword(cub, Keyword.FLYING) shouldBe true
                }
            }

            test("an Equipment on another creature does not switch the Cub on") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skyhunter Cub")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Whispersilk Cloak", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cub = game.findPermanent("Skyhunter Cub").shouldNotBeNull()
                val projected = game.state.projectedState

                withClue("the gate is 'this creature is equipped', not 'you control an Equipment'") {
                    projected.getPower(cub) shouldBe 2
                    projected.getToughness(cub) shouldBe 2
                    projected.hasKeyword(cub, Keyword.FLYING) shouldBe false
                }
            }

            test("an Aura is not Equipment — enchanting the Cub leaves it a 2/2 ground creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skyhunter Cub")
                    .withCardAttachedTo(1, "Pacifism", "Skyhunter Cub")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cub = game.findPermanent("Skyhunter Cub").shouldNotBeNull()
                val projected = game.state.projectedState

                withClue("IsEquipped must not degrade into a generic 'is attached to' check") {
                    projected.getPower(cub) shouldBe 2
                    projected.getToughness(cub) shouldBe 2
                    projected.hasKeyword(cub, Keyword.FLYING) shouldBe false
                }
            }
        }
    }
}
