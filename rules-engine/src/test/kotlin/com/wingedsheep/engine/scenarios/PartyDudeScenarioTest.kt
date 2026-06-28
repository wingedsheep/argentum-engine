package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Regression for Party Dude's Level 2 ability (TMT):
 * "Whenever an artifact an opponent controls is put into a graveyard from the battlefield, draw a card."
 *
 * The original definition let `Triggers.leavesBattlefield` default its binding to `SELF`, so the
 * trigger only watched Party Dude's *own* departure — an opponent's artifact (e.g. a sacrificed
 * Food token) dying never drew a card. The fix uses `TriggerBinding.ANY` so any matching artifact
 * leaving the battlefield fires it.
 */
class PartyDudeScenarioTest : ScenarioTestBase() {

    // A free "destroy all artifacts" sorcery, used to put the opponent's artifact into the
    // graveyard from the battlefield (the trigger condition) without giving player 1 a draw of
    // its own.
    private val artifactWipe = card("Test Artifact Wipe") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy all artifacts."
        spell {
            effect = Effects.DestroyAll(GameObjectFilter.Artifact)
        }
    }

    init {
        cardRegistry.register(artifactWipe)

        test("Level 2 draws when an opponent's artifact is put into the graveyard from the battlefield") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Party Dude", classLevel = 2)
                .withCardOnBattlefield(2, "Treasure") // an artifact the opponent controls
                .withCardInHand(1, "Test Artifact Wipe")
                .withCardInLibrary(1, "Shock")        // the card Party Dude will draw
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Sanity: the draw target starts in the library, not the hand") {
                game.isInHand(1, "Shock") shouldBe false
            }

            game.castSpell(1, "Test Artifact Wipe")
            game.resolveStack()

            withClue("The opponent's artifact was destroyed") {
                game.findPermanent("Treasure").shouldBeNull()
            }
            withClue("Party Dude Level 2 should draw a card when the opponent's artifact dies") {
                game.isInHand(1, "Shock") shouldBe true
            }
        }

        test("Level 1 Party Dude does NOT draw — the draw is a Level 2 ability") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Party Dude", classLevel = 1)
                .withCardOnBattlefield(2, "Treasure")
                .withCardInHand(1, "Test Artifact Wipe")
                .withCardInLibrary(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Artifact Wipe")
            game.resolveStack()

            withClue("At Level 1 the draw ability isn't active yet") {
                game.isInHand(1, "Shock") shouldBe false
            }
        }
    }
}
