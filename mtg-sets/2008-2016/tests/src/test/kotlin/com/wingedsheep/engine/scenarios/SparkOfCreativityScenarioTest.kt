package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * Spark of Creativity (KLD #131, {R} Sorcery).
 *
 *   Choose target creature. Exile the top card of your library. You may have Spark of Creativity
 *   deal damage to that creature equal to the exiled card's mana value. If you don't, you may
 *   play that card until end of turn.
 */
class SparkOfCreativityScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Spark of Creativity")
        .withCardInLibrary(1, "Hill Giant") // mana value 4
        .withCardOnBattlefield(2, "Hill Giant") // 3/3 target
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun canCastExiledGiant(game: ScenarioTestBase.TestGame): Boolean =
        game.getLegalActions(1).any {
            it.actionType == "CastSpell" && it.description.contains("Hill Giant")
        }

    init {
        test("choosing the damage deals the exiled card's mana value and grants no play") {
            val game = setup()
            val target = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Spark of Creativity", target)
            game.resolveStack()

            withClue("the may prompt is offered") { game.hasPendingDecision().shouldBeTrue() }
            game.answerYesNo(true)
            game.resolveStack()

            withClue("4 damage kills the 3/3") { game.isInGraveyard(2, "Hill Giant").shouldBeTrue() }
            game.isInExile(1, "Hill Giant").shouldBeTrue()
            withClue("the damage was chosen, so the card isn't playable") {
                canCastExiledGiant(game).shouldBeFalse()
            }
        }

        test("declining the damage lets you play the exiled card this turn") {
            val game = setup()
            val target = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Spark of Creativity", target)
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            withClue("no damage dealt") { game.isOnBattlefield("Hill Giant").shouldBeTrue() }
            game.isInExile(1, "Hill Giant").shouldBeTrue()
            withClue("the exiled card is playable until end of turn") {
                canCastExiledGiant(game).shouldBeTrue()
            }
        }
    }
}
