package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fall of the Titans (OGW #109, {X}{X}{R}, Instant).
 *
 *   Surge {X}{R}
 *   Fall of the Titans deals X damage to each of up to two targets.
 *
 * The first card with {X} inside its surge cost: a surged cast pays X once, a normal cast pays it
 * twice, and the damage reads the announced X either way.
 */
class FallOfTheTitansScenarioTest : ScenarioTestBase() {

    private fun board(mountains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Fall of the Titans")
        .withCardInHand(1, "Lightning Bolt")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun ScenarioTestBase.TestGame.fallCast(x: Int, surge: Boolean): CastSpell {
        val giant = findPermanent("Hill Giant")
        giant shouldNotBe null
        return CastSpell(
            player1Id, findCardsInHand(1, "Fall of the Titans").first(),
            targets = listOf(ChosenTarget.Permanent(giant!!), ChosenTarget.Player(player2Id)),
            xValue = x,
            useAlternativeCost = surge,
            alternativeCostType = if (surge) AlternativeCostType.SURGE else null,
        )
    }

    init {
        test("surged for {X}{R}: X = 3 off four Mountains deals 3 to each of two targets") {
            val game = board(mountains = 5)
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            // Four Mountains left: the surge cost {3}{R} is payable, the mana cost {3}{3}{R} is not.
            game.execute(game.fallCast(x = 3, surge = true)).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant took 3 and died") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
            withClue("opponent took 3") { game.getLifeTotal(2) shouldBe 14 }
        }

        test("cast for its mana cost the X is paid twice: X = 3 is unaffordable off four Mountains") {
            val game = board(mountains = 4)
            game.execute(game.fallCast(x = 3, surge = false)).error shouldNotBe null
            game.findPermanent("Hill Giant") shouldNotBe null
        }

        test("cast for its mana cost: X = 2 off five Mountains deals 2 to each of two targets") {
            val game = board(mountains = 5)
            game.execute(game.fallCast(x = 2, surge = false)).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant survives 2 damage") { game.findPermanent("Hill Giant") shouldNotBe null }
            game.getLifeTotal(2) shouldBe 18
        }

        test("surge needs another spell cast this turn first") {
            val game = board(mountains = 5)
            game.execute(game.fallCast(x = 1, surge = true)).error shouldNotBe null
        }
    }
}
