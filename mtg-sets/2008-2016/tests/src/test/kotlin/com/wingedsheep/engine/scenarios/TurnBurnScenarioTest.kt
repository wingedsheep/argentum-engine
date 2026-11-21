package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class TurnBurnScenarioTest : ScenarioTestBase() {
    init {
        test("Turn alone makes a creature a red 0/1 Weird with no abilities") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Turn // Burn")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Turn // Burn"
            }
            val angel = game.findPermanent("Serra Angel")!!

            game.execute(
                CastSpell(game.player1Id, cardId, targets = listOf(ChosenTarget.Permanent(angel)), faceIndex = 0)
            ).error.shouldBeNull()
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(angel) shouldBe 0
            projected.getToughness(angel) shouldBe 1
            projected.getColors(angel) shouldBe setOf("RED")
            projected.getSubtypes(angel) shouldBe setOf("Weird")
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
            projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
        }

        test("fused onto the same creature, Burn kills the 0/1 Weird that Turn made") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Turn // Burn")
                .withLandsOnBattlefield(1, "Island", 3).withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Serra Angel")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Turn // Burn"
            }
            val angel = game.findPermanent("Serra Angel")!!

            game.execute(
                CastSpell(
                    game.player1Id, cardId,
                    targets = listOf(ChosenTarget.Permanent(angel), ChosenTarget.Permanent(angel)),
                    castFused = true
                )
            ).error.shouldBeNull()
            game.resolveStack()

            game.findPermanent("Serra Angel").shouldBeNull()
            game.isInGraveyard(2, "Serra Angel") shouldBe true
            game.isInGraveyard(1, "Turn // Burn") shouldBe true
        }

        test("Burn alone deals 2 damage to a player") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Turn // Burn")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val cardId = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Turn // Burn"
            }

            game.execute(
                CastSpell(game.player1Id, cardId, targets = listOf(ChosenTarget.Player(game.player2Id)), faceIndex = 1)
            ).error.shouldBeNull()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
        }
    }
}
