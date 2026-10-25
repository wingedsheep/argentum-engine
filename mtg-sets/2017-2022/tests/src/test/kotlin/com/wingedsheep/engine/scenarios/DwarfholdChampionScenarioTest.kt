package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dwarfhold Champion (AFR #14): "As long as this creature is equipped, it gets +0/+2."
 * Pins that the gate reads this creature's own attachments.
 */
class DwarfholdChampionScenarioTest : ScenarioTestBase() {

    init {
        test("unequipped, it is a plain 3/1") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dwarfhold Champion")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val champion = game.findPermanent("Dwarfhold Champion").shouldNotBeNull()
            val projected = game.state.projectedState
            projected.getPower(champion) shouldBe 3
            projected.getToughness(champion) shouldBe 1
        }

        test("equipped, it is a 3/3") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dwarfhold Champion")
                .withCardAttachedTo(1, "Whispersilk Cloak", "Dwarfhold Champion")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val champion = game.findPermanent("Dwarfhold Champion").shouldNotBeNull()
            val projected = game.state.projectedState
            withClue("Whispersilk Cloak changes no stats itself, so +0/+2 is the Champion's own bonus") {
                projected.getPower(champion) shouldBe 3
                projected.getToughness(champion) shouldBe 3
            }
        }

        test("an Equipment on another creature does not switch the Champion on") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dwarfhold Champion")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Whispersilk Cloak", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val champion = game.findPermanent("Dwarfhold Champion").shouldNotBeNull()
            val projected = game.state.projectedState
            projected.getPower(champion) shouldBe 3
            projected.getToughness(champion) shouldBe 1
        }
    }
}
