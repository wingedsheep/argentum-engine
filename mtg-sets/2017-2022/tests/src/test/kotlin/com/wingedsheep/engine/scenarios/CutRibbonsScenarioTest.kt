package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Cut // Ribbons (AKH).
 *
 * Cut {1}{R}: "Cut deals 4 damage to target creature."
 * Ribbons {X}{B}{B}, Aftermath: "Each opponent loses X life."
 *
 * Ribbons is the first aftermath half with {X} in its cost: the X chosen when it is cast from the
 * graveyard must reach the resolving face's `xValue()`.
 */
class CutRibbonsScenarioTest : ScenarioTestBase() {

    init {
        test("Cut deals 4 to a creature, then Ribbons from the graveyard drains each opponent for X and is exiled") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInHand(1, "Cut // Ribbons")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            val inHand = game.findCardsInHand(1, "Cut // Ribbons").first()
            game.execute(
                CastSpell(game.player1Id, inHand, listOf(ChosenTarget.Permanent(giant)), faceIndex = 0)
            ).error.shouldBeNull()
            game.resolveStack()

            withClue("Cut's 4 damage kills the 3/3 Hill Giant") {
                game.findPermanent("Hill Giant").shouldBeNull()
            }
            game.isInGraveyard(1, "Cut // Ribbons") shouldBe true

            val inGraveyard = game.findCardsInGraveyard(1, "Cut // Ribbons").first()
            game.execute(
                CastSpell(game.player1Id, inGraveyard, xValue = 3, faceIndex = 1)
            ).error.shouldBeNull()
            game.resolveStack()

            withClue("Ribbons with X=3 makes the opponent lose 3 life") {
                game.getLifeTotal(2) shouldBe 17
            }
            withClue("the caster loses no life") {
                game.getLifeTotal(1) shouldBe 20
            }
            withClue("Ribbons, cast from the graveyard, is exiled") {
                game.isInExile(1, "Cut // Ribbons") shouldBe true
            }
        }
    }
}
