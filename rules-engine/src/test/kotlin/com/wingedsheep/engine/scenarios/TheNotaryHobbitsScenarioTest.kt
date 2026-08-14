package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Notary Hobbits (HOB) — "When The Notary Hobbits enter, if they're not a token, create two
 * tokens that are copies of them, except the tokens aren't legendary."
 *
 * The interesting claim is the intervening-if (CR 603.4): every token copy carries the same enters
 * trigger, so without the "if they're not a token" gate two tokens would each make two more and the
 * board would never stop growing. The count assertions below are what pins that down.
 */
class TheNotaryHobbitsScenarioTest : ScenarioTestBase() {

    init {
        test("casting them makes exactly two nonlegendary token copies, and the copies don't recurse") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "The Notary Hobbits")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "The Notary Hobbits").error shouldBe null
            game.resolveStack()

            val copies = game.findPermanents("The Notary Hobbits")
            withClue("the printed card plus two token copies — and no runaway recursion") {
                copies.size shouldBe 3
            }
            copies.count { game.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 2

            // Legend rule (CR 704.5j) would eat a legendary duplicate; the tokens survive it.
            game.checkStateBasedActions()
            game.findPermanents("The Notary Hobbits").size shouldBe 3
        }
    }
}
