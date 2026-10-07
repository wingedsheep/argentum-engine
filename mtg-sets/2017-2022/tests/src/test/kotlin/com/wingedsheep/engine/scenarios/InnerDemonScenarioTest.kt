package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class InnerDemonScenarioTest : ScenarioTestBase() {
    private val projector = StateProjector()

    init {
        test("the granted Demon type protects even an opponent's enchanted creature from the global trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Inner Demon")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Vampire Nighthawk")
                .withCardOnBattlefield(2, "Rune-Scarred Demon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val vampire = game.findPermanent("Vampire Nighthawk")!!
            val demon = game.findPermanent("Rune-Scarred Demon")!!

            game.castSpell(1, "Inner Demon", giant).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            val projected = projector.project(game.state)
            projected.getPower(giant) shouldBe 5
            projected.getToughness(giant) shouldBe 5
            projected.hasKeyword(giant, Keyword.FLYING) shouldBe true
            projected.hasSubtype(giant, "Demon") shouldBe true
            projected.hasSubtype(giant, "Giant") shouldBe true
            projected.getPower(vampire) shouldBe 0
            projected.getToughness(vampire) shouldBe 1
            projected.getPower(demon) shouldBe 6
            projected.getToughness(demon) shouldBe 6
        }

        test("removing the Aura in response lets the trigger affect its former host") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Inner Demon")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(2, "Disenchant")
                .withLandsOnBattlefield(2, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Inner Demon", bears).error shouldBe null
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
            val aura = game.findPermanent("Inner Demon")!!
            game.state.stack.size shouldBe 1
            projector.project(game.state).hasSubtype(bears, "Demon") shouldBe true

            game.passPriority().error shouldBe null
            game.castSpell(2, "Disenchant", aura).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Inner Demon") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }

        test("the trigger snapshots its creatures and expires at cleanup") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Inner Demon")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Rune-Scarred Demon")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val demon = game.findPermanent("Rune-Scarred Demon")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Inner Demon", demon).error shouldBe null
            game.resolveStack()
            projector.project(game.state).getToughness(giant) shouldBe 1

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            projector.project(game.state).getToughness(bears) shouldBe 2
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            projector.project(game.state).getToughness(giant) shouldBe 3
            projector.project(game.state).getToughness(demon) shouldBe 8
        }
    }
}
