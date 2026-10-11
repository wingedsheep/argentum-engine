package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fisher's Talent {2}{G}{U} — Enchantment — Class
 *   At the beginning of your upkeep, look at the top card of your library. You may reveal it if it's
 *   a land card. Create a 1/1 blue Fish creature token if you revealed it this way. Then draw a card.
 *   {G}{U}: Level 2 — If you would create a Fish token, create a 3/3 blue Shark creature token instead.
 *   {2}{G}{U}: Level 3 — If you would create a Shark token, create an 8/8 blue Octopus creature token
 *   instead.
 *
 * Level 3 only bites through level 2: the Fish becomes a Shark, and that substitute Shark is checked
 * again (CR 616.2) and becomes an Octopus.
 */
class FishersTalentScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokens(): List<EntityId> =
        state.getBattlefield().filter { id ->
            val c = state.getEntity(id) ?: return@filter false
            c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == player1Id
        }

    /** Player 1 controls the Class at [level]; [topCard] tops their library. Player 2 is active. */
    private fun gameWith(level: Int, topCard: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Fisher's Talent", classLevel = level)
        .withCardInLibrary(1, topCard)
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .apply { repeat(5) { withCardInLibrary(1, "Grizzly Bears") } }
        .apply { repeat(5) { withCardInLibrary(2, "Grizzly Bears") } }
        .build()

    /** Advance to player 1's upkeep trigger and answer the reveal prompt. */
    private fun TestGame.runUpkeep(reveal: Boolean) {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
        if (hasPendingDecision()) {
            if (reveal) selectCards(listOf(state.getLibrary(player1Id).first())) else skipSelection()
        }
        resolveStack()
    }

    private fun TestGame.singleToken(power: Int, subtype: String): EntityId {
        val tokens = tokens()
        withClue("exactly one token, a $power/$power $subtype") { tokens.size shouldBe 1 }
        val token = tokens.single()
        val projected = state.projectedState
        projected.hasSubtype(token, subtype) shouldBe true
        projected.getPower(token) shouldBe power
        projected.getToughness(token) shouldBe power
        projected.hasColor(token, Color.BLUE) shouldBe true
        return token
    }

    init {
        test("level 1: revealing a land makes a 1/1 Fish, then draws the card") {
            val game = gameWith(level = 1, topCard = "Forest")
            val handBefore = game.handSize(1)

            game.runUpkeep(reveal = true)

            game.singleToken(1, "Fish")
            withClue("then draw a card — the revealed land") {
                game.handSize(1) shouldBe handBefore + 1
                game.findCardsInHand(1, "Forest").size shouldBe 1
            }
        }

        test("declining to reveal makes no Fish but still draws") {
            val game = gameWith(level = 1, topCard = "Forest")
            val handBefore = game.handSize(1)

            game.runUpkeep(reveal = false)

            game.tokens().size shouldBe 0
            game.handSize(1) shouldBe handBefore + 1
        }

        test("a nonland top card can't be revealed: no Fish, still draws") {
            val game = gameWith(level = 1, topCard = "Lightning Bolt")
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            if (game.hasPendingDecision()) {
                withClue("the nonland card can't be chosen to reveal") {
                    game.selectCards(listOf(game.state.getLibrary(game.player1Id).first())).error shouldNotBe null
                }
                game.skipSelection()
            }
            game.resolveStack()

            game.tokens().size shouldBe 0
            game.handSize(1) shouldBe handBefore + 1
        }

        test("level 2: the Fish is replaced by a 3/3 blue Shark") {
            val game = gameWith(level = 2, topCard = "Forest")
            game.runUpkeep(reveal = true)
            game.singleToken(3, "Shark")
        }

        test("level 3: the Fish becomes a Shark, and that Shark becomes an 8/8 Octopus") {
            val game = gameWith(level = 3, topCard = "Forest")
            game.runUpkeep(reveal = true)
            val octopus = game.singleToken(8, "Octopus")
            withClue("the Octopus is not also a Shark or Fish") {
                game.state.projectedState.hasSubtype(octopus, "Shark") shouldBe false
                game.state.projectedState.hasSubtype(octopus, "Fish") shouldBe false
            }
        }

        test("gaining level 3 by activation turns on the Shark → Octopus substitution") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Fisher's Talent", classLevel = 2)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(5) { withCardInLibrary(1, "Grizzly Bears") } }
                .apply { repeat(5) { withCardInLibrary(2, "Grizzly Bears") } }
                .build()
            val talent = game.findPermanent("Fisher's Talent").shouldNotBeNull()

            game.execute(ActivateAbility(game.player1Id, talent, AbilityId.classLevelUp(3))).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            // Through the opponent's turn to player 1's next upkeep.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.runUpkeep(reveal = true)
            game.singleToken(8, "Octopus")
        }
    }
}
