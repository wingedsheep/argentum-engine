package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Xathrid Slyblade — {2}{B} 2/1 Human Assassin. "Hexproof. {3}{B}: Until end of turn, this creature
 * loses hexproof and gains first strike and deathtouch."
 *
 * The removal of its own printed hexproof must be real: once the ability resolves, an opponent can
 * target it, and the keyword comes back next turn.
 */
class XathridSlybladeScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Xathrid Slyblade")!!.script.activatedAbilities.single().id

        fun setup(opponentHasPriority: Boolean = false) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Xathrid Slyblade")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withCardInHand(2, "Lightning Bolt")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withActivePlayer(1)
            .withPriorityPlayer(if (opponentHasPriority) 2 else 1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("hexproof stops the opponent from targeting it") {
            val game = setup(opponentHasPriority = true)
            val slyblade = game.findPermanent("Xathrid Slyblade")!!

            game.state.projectedState.hasKeyword(slyblade, Keyword.HEXPROOF) shouldBe true
            withClue("Lightning Bolt must not be able to target a hexproof Slyblade") {
                game.castSpell(2, "Lightning Bolt", slyblade).error shouldNotBe null
            }
        }

        test("the ability strips hexproof and grants first strike and deathtouch until end of turn") {
            val game = setup()
            val slyblade = game.findPermanent("Xathrid Slyblade")!!

            val result = game.execute(ActivateAbility(game.player1Id, slyblade, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            projected.hasKeyword(slyblade, Keyword.HEXPROOF) shouldBe false
            projected.hasKeyword(slyblade, Keyword.FIRST_STRIKE) shouldBe true
            projected.hasKeyword(slyblade, Keyword.DEATHTOUCH) shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val next = game.state.projectedState
            next.hasKeyword(slyblade, Keyword.HEXPROOF) shouldBe true
            next.hasKeyword(slyblade, Keyword.FIRST_STRIKE) shouldBe false
            next.hasKeyword(slyblade, Keyword.DEATHTOUCH) shouldBe false
        }

        test("once it has lost hexproof, the opponent can Bolt it") {
            val game = setup()
            val slyblade = game.findPermanent("Xathrid Slyblade")!!

            game.execute(ActivateAbility(game.player1Id, slyblade, abilityId)).error shouldBe null
            game.resolveStack()

            game.passPriority()
            val bolt = game.castSpell(2, "Lightning Bolt", slyblade)
            withClue("Bolt should be able to target the Slyblade now: ${bolt.error}") { bolt.error shouldBe null }
            game.resolveStack()

            game.findPermanent("Xathrid Slyblade") shouldBe null
        }
    }
}
