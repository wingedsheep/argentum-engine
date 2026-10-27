package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.IcebreakerKraken
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Icebreaker Kraken (KHM #63) — affinity for snow lands; on entering, artifacts and creatures target
 * opponent controls don't untap during that player's next untap step (lands still do, and nothing
 * is tapped by it); three snow lands bounce it back to hand.
 */
class IcebreakerKrakenScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Icebreaker Kraken")
        .withLandsOnBattlefield(1, "Snow-Covered Island", 6)
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
        .withCardOnBattlefield(2, "Ornithopter", tapped = true)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Forest", tapped = true)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castKraken() {
        castSpell(1, "Icebreaker Kraken").error shouldBe null
        resolveStack()
        if (hasPendingDecision()) selectTargets(listOf(player2Id)).error shouldBe null
        resolveStack()
    }

    private fun TestGame.tapped(name: String) = state.getEntity(findPermanent(name)!!)!!.has<TappedComponent>()

    init {
        test("costs {1} less per snow land, and holds the opponent's artifacts and creatures through their untap step") {
            val game = game()
            game.castKraken()
            game.isOnBattlefield("Icebreaker Kraken") shouldBe true
            // Taps nothing: the untapped Hill Giant stays untapped.
            game.tapped("Hill Giant") shouldBe false

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.tapped("Grizzly Bears") shouldBe true
            game.tapped("Ornithopter") shouldBe true
            game.tapped("Forest") shouldBe false
        }

        test("returning three snow lands bounces it to its owner's hand") {
            val game = game()
            game.castKraken()
            val kraken = game.findPermanent("Icebreaker Kraken")!!
            val lands = game.findAllPermanents("Snow-Covered Island").take(3)
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = kraken,
                    abilityId = IcebreakerKraken.activatedAbilities.single().id,
                    costPayment = AdditionalCostPayment(bouncedPermanents = lands),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Icebreaker Kraken") shouldBe true
            game.findCardsInHand(1, "Snow-Covered Island").size shouldBe 3
        }
    }
}
