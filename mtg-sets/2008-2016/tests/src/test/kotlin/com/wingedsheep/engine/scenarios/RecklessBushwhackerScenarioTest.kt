package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reckless Bushwhacker (OGW #116, {2}{R}, 2/1 Goblin Warrior Ally).
 *
 *   Surge {1}{R}
 *   Haste
 *   When this creature enters, if its surge cost was paid, other creatures you control get +1/+0
 *   and gain haste until end of turn.
 */
class RecklessBushwhackerScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Reckless Bushwhacker")
        .withCardInHand(1, "Lightning Bolt")
        .withCardOnBattlefield(1, "Glory Seeker", summoningSickness = true)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.surge(name: String) = execute(
        CastSpell(
            player1Id, findCardsInHand(1, name).first(),
            useAlternativeCost = true, alternativeCostType = AlternativeCostType.SURGE,
        )
    )

    init {
        test("surged after Lightning Bolt: the other creatures get +1/+0 and haste, Bushwhacker itself doesn't") {
            val game = board()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.surge("Reckless Bushwhacker").error shouldBe null
            game.resolveStack()

            val seeker = game.findPermanent("Glory Seeker")!!
            val bushwhacker = game.findPermanent("Reckless Bushwhacker")!!
            val projected = game.state.projectedState
            withClue("Glory Seeker 2/2 → 3/2 with haste") {
                projected.getPower(seeker) shouldBe 3
                projected.hasKeyword(seeker, Keyword.HASTE) shouldBe true
            }
            withClue("\"other creatures\" — Bushwhacker keeps its printed 2 power") {
                projected.getPower(bushwhacker) shouldBe 2
            }
            game.getLifeTotal(2) shouldBe 17
        }

        test("cast for its mana cost: no pump") {
            val game = board()
            game.castSpell(1, "Reckless Bushwhacker").error shouldBe null
            game.resolveStack()

            val seeker = game.findPermanent("Glory Seeker")!!
            game.findPermanent("Reckless Bushwhacker") shouldNotBe null
            game.state.projectedState.getPower(seeker) shouldBe 2
            game.state.projectedState.hasKeyword(seeker, Keyword.HASTE) shouldBe false
        }

        test("surge can't be paid before another spell was cast this turn") {
            val game = board()
            game.surge("Reckless Bushwhacker").error shouldNotBe null
            game.isInHand(1, "Reckless Bushwhacker") shouldBe true
        }
    }
}
