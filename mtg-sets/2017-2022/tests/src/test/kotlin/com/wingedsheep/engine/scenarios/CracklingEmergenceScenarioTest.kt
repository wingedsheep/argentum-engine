package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Crackling Emergence (NEO #136): "Enchant land you control. Enchanted land is a 3/3 red Spirit
 * creature with haste. It's still a land. If enchanted land would be destroyed, instead sacrifice
 * this Aura and that land gains indestructible until end of turn."
 *
 * The replacement's own rules live in `ReplaceDestructionWithTest`; this pins the card.
 */
class CracklingEmergenceScenarioTest : ScenarioTestBase() {

    init {
        context("Crackling Emergence") {

            test("cast on a land you control, the land becomes a 3/3 red Spirit creature with haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crackling Emergence")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findAllPermanents("Mountain").first()

                game.castSpell(1, "Crackling Emergence", land).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.isCreature(land) shouldBe true
                projected.hasType(land, "LAND") shouldBe true
                projected.getPower(land) shouldBe 3
                projected.getToughness(land) shouldBe 3
                projected.hasSubtype(land, "Spirit") shouldBe true
                projected.hasSubtype(land, "Mountain") shouldBe true
                projected.getColors(land) shouldBe setOf("RED")
                projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            }

            test("lethal damage to the enchanted land sacrifices the Aura instead, and the land is indestructible") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardAttachedTo(1, "Crackling Emergence", "Mountain")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findAllPermanents("Mountain").first { game.state.projectedState.isCreature(it) }

                game.castSpell(2, "Lightning Bolt", land).error shouldBe null
                game.resolveStack()

                game.state.getBattlefield().contains(land) shouldBe true
                game.isInGraveyard(1, "Crackling Emergence") shouldBe true
                game.state.projectedState.isCreature(land) shouldBe false
                game.state.projectedState.hasKeyword(land, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            test("a land destruction spell is replaced the same way") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Crackling Emergence", "Forest")
                    .withCardInHand(2, "Stone Rain")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findPermanent("Forest")!!

                game.castSpell(2, "Stone Rain", land).error shouldBe null
                game.resolveStack()

                game.state.getBattlefield().contains(land) shouldBe true
                game.isInGraveyard(1, "Crackling Emergence") shouldBe true
                game.state.projectedState.hasKeyword(land, Keyword.INDESTRUCTIBLE) shouldBe true
            }
        }
    }
}
