package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Silverfur Partisan (SOI #228) — {2}{G} Creature — Wolf Warrior, 2/2.
 *
 * "Trample. Whenever a Wolf or Werewolf you control becomes the target of an instant or sorcery
 * spell, create a 2/2 green Wolf creature token."
 *
 * The trigger narrows both halves of the targeting event: *what* got targeted (a Wolf or Werewolf
 * you control — the Partisan itself or another one) and *what* did the targeting (an instant or
 * sorcery spell, cast by any player). The non-Wolf case proves the subject filter isn't a no-op.
 */
class SilverfurPartisanScenarioTest : ScenarioTestBase() {

    init {
        context("Silverfur Partisan") {

            test("an instant you cast targeting another Werewolf you control creates a Wolf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Silverfur Partisan")
                    .withCardOnBattlefield(1, "Kruin Outlaw")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val outlaw = game.findPermanent("Kruin Outlaw")!!
                game.castSpell(1, "Giant Growth", targetId = outlaw).error shouldBe null
                game.resolveStack()

                val wolves = game.findPermanents("Wolf Token")
                withClue("one 2/2 Wolf token was created") {
                    wolves shouldHaveSize 1
                    game.state.projectedState.getPower(wolves.single()) shouldBe 2
                    game.state.projectedState.getToughness(wolves.single()) shouldBe 2
                }
            }

            test("an opponent's instant targeting the Partisan itself also creates a Wolf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Silverfur Partisan")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val partisan = game.findPermanent("Silverfur Partisan")!!
                game.castSpell(2, "Shock", targetId = partisan).error shouldBe null
                game.resolveStack()

                withClue("the trigger resolves before Shock, so the token is made even though the Partisan dies") {
                    game.findPermanents("Wolf Token") shouldHaveSize 1
                    game.findPermanent("Silverfur Partisan") shouldBe null
                }
            }

            test("targeting a creature that is neither a Wolf nor a Werewolf creates no token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Silverfur Partisan")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Giant Growth", targetId = bears).error shouldBe null
                game.resolveStack()

                game.findPermanents("Wolf Token") shouldHaveSize 0
            }
        }
    }
}
