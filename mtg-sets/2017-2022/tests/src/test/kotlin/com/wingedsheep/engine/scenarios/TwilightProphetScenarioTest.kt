package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Twilight Prophet (Rivals of Ixalan #88) — {2}{B}{B} Creature — Vampire Cleric 2/4.
 *   "At the beginning of your upkeep, if you have the city's blessing, reveal the top card of your
 *    library and put it into your hand. Each opponent loses X life and you gain X life, where X is
 *    that card's mana value."
 *
 * Pins the city's blessing as an *intervening-if* on a triggered ability (no other card gates a
 * trigger on it), and the revealed card's mana value read twice from the pipeline's collection —
 * once for the opponents' loss, once for your gain.
 */
class TwilightProphetScenarioTest : ScenarioTestBase() {

    init {
        test("with the city's blessing, upkeep draws the top card and drains its mana value") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .withCardOnBattlefield(1, "Twilight Prophet")
                .withLandsOnBattlefield(1, "Swamp", 9) // ten permanents: ascend grants the blessing
                .withCardInLibrary(1, "Hill Giant") // mana value 4 ({3}{R})
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            withClue("the revealed top card is now in hand") {
                game.isInHand(1, "Hill Giant") shouldBe true
            }
            withClue("each opponent loses X = 4") {
                game.getLifeTotal(2) shouldBe 16
            }
            withClue("you gain X = 4") {
                game.getLifeTotal(1) shouldBe 24
            }
        }

        test("without the city's blessing the upkeep ability does not trigger") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .withCardOnBattlefield(1, "Twilight Prophet")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Hill Giant")
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            withClue("the top card stays in the library") {
                game.isInHand(1, "Hill Giant") shouldBe false
            }
            withClue("no life changes hands") {
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
