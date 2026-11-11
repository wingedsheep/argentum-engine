package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ondu Rising — "Whenever a creature attacks this turn, it gains lifelink until end of turn."
 * Awakened, the hasty land it animates attacks with lifelink too (ruling).
 */
class OnduRisingScenarioTest : ScenarioTestBase() {

    init {
        test("an attacking creature gains lifelink as it attacks, and its combat damage gains you life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Ondu Rising")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Ondu Rising").error shouldBe null
            game.resolveStack()

            withClue("nothing has lifelink before it attacks") {
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the attacker gains lifelink; the creature that stayed home doesn't") {
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                game.state.projectedState.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
            }

            // The opponent has no creatures, so the declare blockers step has no blocks to make.
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("cast for its awaken cost, the hasty 4/4 land attacks the same turn with lifelink") {
            // Maze of Ith has no mana ability, so paying {4}{W} with the Plains leaves it untapped.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Ondu Rising")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Maze of Ith")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val land = game.findPermanent("Maze of Ith")!!

            game.castSpellWithAwaken(1, "Ondu Rising", listOf(land)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 4
            projected.hasKeyword(land, Keyword.LIFELINK) shouldBe false

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(land to game.player2Id))).error shouldBe null
            game.resolveStack()
            game.state.projectedState.hasKeyword(land, Keyword.LIFELINK) shouldBe true

            // The opponent has no creatures, so the declare blockers step has no blocks to make.
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 24
        }
    }
}
