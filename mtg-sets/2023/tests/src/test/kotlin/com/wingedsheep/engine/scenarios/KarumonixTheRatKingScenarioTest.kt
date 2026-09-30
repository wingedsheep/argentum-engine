package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import io.kotest.matchers.shouldBe

/**
 * Karumonix, the Rat King (ONE #98) — {1}{B}{B} 3/3 Legendary Creature — Phyrexian Rat.
 *
 * "Toxic 1. Other Rats you control have toxic 1. When Karumonix enters, look at the top five
 * cards of your library. You may reveal any number of Rat cards from among them and put the
 * revealed cards into your hand. Put the rest on the bottom of your library in a random order."
 */
class KarumonixTheRatKingScenarioTest : ScenarioTestBase() {

    private fun TestGame.poison(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    private fun TestGame.resolveUntilDecision() {
        var guard = 0
        while (!hasPendingDecision() && state.stack.isNotEmpty() && guard++ < 10) resolveStack()
    }

    init {
        test("other Rats you control gain toxic 1, cumulative with printed toxic") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Karumonix, the Rat King")
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withCardOnBattlefield(2, "Blightbelly Rat")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            val myRat = game.findPermanents("Blightbelly Rat").first {
                game.state.getZone(ZoneKey(game.player1Id, Zone.BATTLEFIELD)).contains(it)
            }
            val theirRat = game.findPermanents("Blightbelly Rat").first { it != myRat }
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, "TOXIC_1") shouldBe false
            projected.hasKeyword(theirRat, "TOXIC_2") shouldBe false

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Karumonix, the Rat King" to 2, "Blightbelly Rat" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            // Karumonix: toxic 1. Blightbelly Rat: printed toxic 1 + granted toxic 1 = 2.
            game.poison(game.player2Id) shouldBe 3
        }

        test("enters: reveal any number of Rat cards to hand, rest to the bottom") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Karumonix, the Rat King")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Blightbelly Rat")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Blightbelly Rat")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Karumonix, the Rat King").error shouldBe null
            game.resolveUntilDecision()

            game.hasPendingDecision() shouldBe true
            val rats = game.findCardsInLibrary(1, "Blightbelly Rat")
            rats.size shouldBe 2
            game.selectCards(rats).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Blightbelly Rat").size shouldBe 2
            game.librarySize(1) shouldBe 3
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
        }

        test("enters: may reveal no Rats") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Karumonix, the Rat King")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Blightbelly Rat")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Karumonix, the Rat King").error shouldBe null
            game.resolveUntilDecision()
            game.hasPendingDecision() shouldBe true
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 2
        }
    }
}
