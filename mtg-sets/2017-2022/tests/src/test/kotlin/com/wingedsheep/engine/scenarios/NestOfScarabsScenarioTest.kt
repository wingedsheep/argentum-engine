package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nest of Scarabs (AKH #101).
 *
 *   Whenever you put one or more -1/-1 counters on a creature, create that many 1/1 black Insect
 *   creature tokens.
 *
 * Proves "that many" reads the number of counters the placement put (three from Stinging Shot, even
 * though the 2/2 only needed two to die — ruling 2017-04-18), that an opponent's creature counts,
 * and that counters an opponent puts don't trigger it.
 */
class NestOfScarabsScenarioTest : ScenarioTestBase() {

    init {
        test("putting three -1/-1 counters on an opponent's creature creates three Insects") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nest of Scarabs")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Stinging Shot")
                .withCardOnBattlefield(2, "Wind Drake")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Stinging Shot", drake).error shouldBe null
            game.resolveStack()

            val insects = game.findPermanents("Insect Token")
            withClue("one Insect per counter placed, all under the Nest's controller") {
                insects.size shouldBe 3
                insects.all {
                    game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id
                } shouldBe true
            }
            game.isOnBattlefield("Wind Drake") shouldBe false
        }

        test("-1/-1 counters an opponent puts on your creature don't trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nest of Scarabs")
                .withCardOnBattlefield(1, "Wind Drake")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardInHand(2, "Stinging Shot")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(2, "Stinging Shot", drake).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wind Drake") shouldBe false
            game.findPermanents("Insect Token").size shouldBe 0
        }
    }
}
