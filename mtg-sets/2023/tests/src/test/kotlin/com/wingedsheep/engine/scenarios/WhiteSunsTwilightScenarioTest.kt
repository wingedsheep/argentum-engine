package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * White Sun's Twilight (ONE #38) — {X}{W}{W} Sorcery.
 *
 *   You gain X life. Create X 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1
 *   and "This token can't block." If X is 5 or more, destroy all other creatures.
 */
class WhiteSunsTwilightScenarioTest : ScenarioTestBase() {

    init {
        test("X below 5 gains X life and creates X Mites without destroying anything") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "White Sun's Twilight")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "White Sun's Twilight", 3).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 23
            val mites = game.findAllPermanents("Phyrexian Mite")
            mites shouldHaveSize 3
            mites.forEach { mite ->
                game.state.projectedState.getController(mite) shouldBe game.player1Id
                game.state.projectedState.hasKeyword(mite, Keyword.TOXIC) shouldBe true
            }
            withClue("no wipe below X = 5") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("X of 5 or more destroys all other creatures but spares the new Mites") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "White Sun's Twilight")
                .withLandsOnBattlefield(1, "Plains", 7)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "White Sun's Twilight", 5).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 25
            game.findAllPermanents("Phyrexian Mite") shouldHaveSize 5
            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }
    }
}
