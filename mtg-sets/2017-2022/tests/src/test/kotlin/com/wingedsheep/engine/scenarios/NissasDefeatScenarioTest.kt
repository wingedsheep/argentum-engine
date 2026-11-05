package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nissa's Defeat (HOU #123) — {2}{G} Sorcery.
 *
 *   Destroy target Forest, green enchantment, or green planeswalker. If that permanent was a Nissa
 *   planeswalker, draw a card.
 *
 * The Nissa check is read before the destroy; only that branch draws.
 */
class NissasDefeatScenarioTest : ScenarioTestBase() {

    init {
        fun board(victim: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Nissa's Defeat")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardOnBattlefield(2, victim)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("destroys a Forest without drawing") {
            val game = board("Forest")
            // Player 1 also has Forests (to pay for the spell): target the opponent's.
            val target = game.findPermanents("Forest").first { id ->
                game.state.getEntity(id)?.get<ControllerComponent>()?.playerId == game.player2Id
            }

            game.castSpell(1, "Nissa's Defeat", targetId = target).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Forest") shouldBe true
            game.handSize(1) shouldBe 0
        }

        test("destroying a Nissa planeswalker draws a card") {
            val game = board("Nissa, Who Shakes the World")

            game.castSpell(1, "Nissa's Defeat", targetId = game.findPermanent("Nissa, Who Shakes the World")!!)
                .error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Nissa, Who Shakes the World") shouldBe true
            withClue("the caster draws one card") { game.handSize(1) shouldBe 1 }
        }

        test("a green planeswalker that isn't a Nissa is destroyed without drawing") {
            val game = board("Garruk Wildspeaker")

            game.castSpell(1, "Nissa's Defeat", targetId = game.findPermanent("Garruk Wildspeaker")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Garruk Wildspeaker") shouldBe true
            game.handSize(1) shouldBe 0
        }

        test("a green creature isn't a legal target") {
            val game = board("Grizzly Bears")

            game.castSpell(1, "Nissa's Defeat", targetId = game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
