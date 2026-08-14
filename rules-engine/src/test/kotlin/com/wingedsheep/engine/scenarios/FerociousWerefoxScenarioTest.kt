package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ferocious Werefox // Guard Change (WOE #170).
 *
 * Creature face: {3}{G} 4/3 Elf Fox Warrior with trample.
 * Adventure face: Guard Change {1}{G}, Instant — Adventure — "Create a Monster Role token attached
 * to target creature you control." (Enchanted creature gets +1/+1 and has trample.)
 */
class FerociousWerefoxScenarioTest : ScenarioTestBase() {

    init {
        test("Guard Change attaches a Monster Role, giving the host +1/+1 and trample") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ferocious Werefox")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val werefox = game.findCardsInHand(1, "Ferocious Werefox").single()
            val bears = game.findPermanent("Grizzly Bears")!!

            // faceIndex = 0 is the Adventure face (CR 715).
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = werefox,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    faceIndex = 0
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("a Monster Role token is attached to Grizzly Bears") {
                game.findPermanent("Monster Role") shouldNotBe null
            }
            withClue("the enchanted 2/2 is now a 3/3 with trample") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }
            withClue("the Adventure exiled itself (CR 715.3d)") {
                game.isInExile(1, "Ferocious Werefox") shouldBe true
            }
        }

        test("the creature face can be cast from exile afterwards as a 4/3 trampler") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Ferocious Werefox")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val werefox = game.findCardsInHand(1, "Ferocious Werefox").single()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = werefox,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    faceIndex = 0
                )
            ).error shouldBe null
            game.resolveStack()
            game.isInExile(1, "Ferocious Werefox") shouldBe true

            // Cast the creature face from exile — {3}{G}.
            game.execute(CastSpell(playerId = game.player1Id, cardId = werefox)).error shouldBe null
            game.resolveStack()

            withClue("the Werefox is on the battlefield as a 4/3 trampler") {
                game.isOnBattlefield("Ferocious Werefox") shouldBe true
                game.state.projectedState.getPower(werefox) shouldBe 4
                game.state.projectedState.getToughness(werefox) shouldBe 3
                game.state.projectedState.hasKeyword(werefox, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
