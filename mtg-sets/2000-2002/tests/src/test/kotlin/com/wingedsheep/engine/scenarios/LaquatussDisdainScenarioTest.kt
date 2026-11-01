package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Laquatus's Disdain (JUD) — "Counter target spell cast from a graveyard. Draw a
 * card."
 *
 * The restriction is the card: it answers flashback and other graveyard casts, and nothing cast
 * from a hand. Enforced through `StatePredicate.WasCast(Zone.GRAVEYARD)`, which reads the
 * `castFromZone` the engine stamps on the spell.
 */
class LaquatussDisdainScenarioTest : ScenarioTestBase() {

    init {
        test("counters a spell cast from a graveyard, and draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Laquatus's Disdain")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island")
                .withCardInGraveyard(2, "Dreams of Laguna")
                .withLandsOnBattlefield(2, "Island", 4) // Flashback {3}{U}
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val flashback = game.castSpellFromGraveyard(2, "Dreams of Laguna")
            withClue("Flashback-casting Dreams of Laguna should succeed: ${flashback.error}") {
                flashback.error shouldBe null
            }
            game.passPriority()

            val handBefore = game.handSize(1)
            val disdain = game.castSpellTargetingStackSpell(1, "Laquatus's Disdain", "Dreams of Laguna")
            withClue("A graveyard-cast spell is a legal target: ${disdain.error}") {
                disdain.error shouldBe null
            }
            game.resolveStack()

            withClue("Dreams of Laguna is countered") {
                game.isInGraveyard(2, "Dreams of Laguna") shouldBe true
            }
            withClue("Laquatus's Disdain draws a card (hand: -1 for the Disdain, +1 drawn)") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("a spell cast from a hand is not a legal target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Laquatus's Disdain")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.castSpell(2, "Grizzly Bears")
            withClue("Casting Grizzly Bears should succeed: ${bears.error}") {
                bears.error shouldBe null
            }
            game.passPriority()

            val disdain = game.castSpellTargetingStackSpell(1, "Laquatus's Disdain", "Grizzly Bears")
            withClue("A hand-cast spell is not a legal target for Laquatus's Disdain") {
                disdain.error shouldNotBe null
            }
            game.resolveStack()
            withClue("Grizzly Bears resolves") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
