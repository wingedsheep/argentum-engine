package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nissa's Zendikon (ZNR #197): "Enchant land. Enchanted land is a 4/4 Elemental creature with
 * reach and haste. It's still a land. When enchanted land dies, return that card to its owner's
 * hand."
 *
 * Pins the dies trigger on an animated land: the Aura looks back at the land it was attached to,
 * and the returned card is the land itself.
 */
class NissasZendikonScenarioTest : ScenarioTestBase() {

    init {
        context("Nissa's Zendikon") {

            test("the enchanted land is a 4/4 Elemental creature land with reach and haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Nissa's Zendikon")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findAllPermanents("Forest").first()

                game.castSpell(1, "Nissa's Zendikon", land).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.isCreature(land) shouldBe true
                projected.hasType(land, "LAND") shouldBe true
                projected.hasSubtype(land, "Forest") shouldBe true
                projected.hasSubtype(land, "Elemental") shouldBe true
                projected.getPower(land) shouldBe 4
                projected.getToughness(land) shouldBe 4
                projected.hasKeyword(land, Keyword.REACH) shouldBe true
                projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            }

            test("when the enchanted land dies, the land card returns to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Nissa's Zendikon", "Forest")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val land = game.findPermanent("Forest")!!

                game.castSpell(2, "Murder", land).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Forest") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe false
                game.isInGraveyard(1, "Nissa's Zendikon") shouldBe true
            }
        }
    }
}
