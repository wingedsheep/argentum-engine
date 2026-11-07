package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.ShardlessOutlander
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Shardless Outlander (J25 #28) — "Trample. Basic landcycling {2}."
 */
class ShardlessOutlanderScenarioTest : FunSpec({

    test("basic landcycling discards it and fetches a basic land, not a nonland card") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ShardlessOutlander)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val outlander = d.putCardInHand(you, "Shardless Outlander")
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        val bears = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        d.giveColorlessMana(you, 2)

        d.submit(TypecycleCard(playerId = you, cardId = outlander)).error shouldBe null
        d.getGraveyardCardNames(you) shouldContain "Shardless Outlander"

        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain forest
        decision.options shouldNotContain bears

        d.submitDecision(you, CardsSelectedResponse(decision.id, listOf(forest)))
        d.findCardInHand(you, "Forest") shouldBe forest
    }
})
