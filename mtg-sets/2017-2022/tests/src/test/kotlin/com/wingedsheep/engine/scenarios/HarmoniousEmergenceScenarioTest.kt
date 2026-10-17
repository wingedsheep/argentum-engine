package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Harmonious Emergence (NEO #190): "Enchant land you control. Enchanted land is a 4/5 green Spirit
 * creature with vigilance and haste. It's still a land. If enchanted land would be destroyed,
 * instead sacrifice this Aura and that land gains indestructible until end of turn."
 *
 * The replacement's own rules live in `ReplaceDestructionWithTest`; this pins the card.
 */
class HarmoniousEmergenceScenarioTest : ScenarioTestBase() {

    init {
        context("Harmonious Emergence") {

            test("the enchanted land is a 4/5 green Spirit creature with vigilance and haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Harmonious Emergence", "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findPermanent("Forest")!!

                val projected = game.state.projectedState
                projected.isCreature(land) shouldBe true
                projected.hasType(land, "LAND") shouldBe true
                projected.getPower(land) shouldBe 4
                projected.getToughness(land) shouldBe 5
                projected.hasSubtype(land, "Spirit") shouldBe true
                projected.getColors(land) shouldBe setOf("GREEN")
                projected.hasKeyword(land, Keyword.VIGILANCE) shouldBe true
                projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            }

            test("Wrath of God sacrifices the Aura instead of destroying the land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Harmonious Emergence", "Forest")
                    .withCardInHand(2, "Wrath of God")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findPermanent("Forest")!!

                game.castSpell(2, "Wrath of God").error shouldBe null
                game.resolveStack()

                game.state.getBattlefield().contains(land) shouldBe true
                game.isInGraveyard(1, "Harmonious Emergence") shouldBe true
                game.state.projectedState.isCreature(land) shouldBe false
                game.state.projectedState.hasKeyword(land, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }
    }
}
