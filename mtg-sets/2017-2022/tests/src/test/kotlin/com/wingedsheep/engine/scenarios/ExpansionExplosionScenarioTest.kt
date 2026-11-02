package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Expansion // Explosion (GRN 224).
 *
 * Explosion {X}{U}{U}{R}{R}: "Explosion deals X damage to any target. Target player draws X cards."
 *
 * Pinned: Explosion is the first split half with {X} in its cost — the announced X is charged on top
 * of the half's own cost and reaches both of its effects at resolution.
 */
class ExpansionExplosionScenarioTest : ScenarioTestBase() {

    init {
        test("Explosion with X=2 deals 2 damage to one target and makes the target player draw 2") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Expansion // Explosion")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cardId = game.findCardsInHand(1, "Expansion // Explosion").single()
            val handBefore = game.handSize(1)
            game.execute(
                CastSpell(
                    game.player1Id,
                    cardId,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Player(game.player1Id)),
                    xValue = 2,
                    faceIndex = 1,
                )
            ).error.shouldBeNull()
            game.resolveStack()

            withClue("Explosion dealt X=2 damage to the opponent") { game.getLifeTotal(2) shouldBe 18 }
            withClue("the caster drew X=2 cards (Explosion itself left the hand)") {
                game.handSize(1) shouldBe handBefore - 1 + 2
            }
            withClue("the opponent drew nothing") { game.handSize(2) shouldBe 0 }
            game.isInGraveyard(1, "Expansion // Explosion") shouldBe true
        }
    }
}
