package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class TrickstersElkScenarioTest : ScenarioTestBase() {

    private fun TestGame.bestowOnto(host: EntityId) {
        execute(
            CastSpell(
                playerId = player1Id,
                cardId = findCardsInHand(1, "Trickster's Elk").single(),
                targets = listOf(ChosenTarget.Permanent(host)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW
            )
        ).error shouldBe null
        resolveStack()
    }

    init {
        test("cast as a creature, it is a plain 3/3 green Elk that changes nothing else") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Trickster's Elk")
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elk = game.findPermanent("Trickster's Elk")!!
            val thopter = game.findPermanent("Ornithopter")!!
            val projected = game.state.projectedState

            projected.getPower(elk) shouldBe 3
            projected.getToughness(elk) shouldBe 3
            projected.isCreature(elk) shouldBe true
            projected.hasType(elk, "ENCHANTMENT") shouldBe true

            projected.getPower(thopter) shouldBe 0
            projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            projected.hasType(thopter, "ARTIFACT") shouldBe true
        }

        test("bestowed, the enchanted creature loses all abilities and is a 3/3 green Elk creature only") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Trickster's Elk")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val thopter = game.findPermanent("Ornithopter")!!
            game.bestowOnto(thopter)

            val elk = game.findPermanent("Trickster's Elk")!!
            val projected = game.state.projectedState
            projected.getPower(thopter) shouldBe 3
            projected.getToughness(thopter) shouldBe 3
            projected.hasKeyword(thopter, Keyword.FLYING) shouldBe false
            projected.hasLostAllAbilities(thopter) shouldBe true
            projected.isCreature(thopter) shouldBe true
            projected.hasType(thopter, "ARTIFACT") shouldBe false
            projected.getSubtypes(thopter) shouldContainExactly setOf("Elk")
            projected.getColors(thopter) shouldContainExactly setOf(Color.GREEN.name)

            // As an attached Aura, the Elk itself is not a creature.
            projected.isCreature(elk) shouldBe false
        }

        test("when the enchanted creature leaves, the Elk becomes a 3/3 creature again") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Trickster's Elk")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val thopter = game.findPermanent("Ornithopter")!!
            game.bestowOnto(thopter)

            // The Elk made it a 3/3, so 3 damage is lethal.
            game.castSpell(1, "Lightning Bolt", thopter).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            val elk = game.findPermanent("Trickster's Elk")!!
            val projected = game.state.projectedState
            projected.isCreature(elk) shouldBe true
            projected.getPower(elk) shouldBe 3
            projected.getToughness(elk) shouldBe 3
        }
    }
}
