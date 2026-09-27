package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.ManaSeism
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mana Seism (CHK #179) — {1}{R} Sorcery.
 *
 * "Sacrifice any number of lands, then add that much {C}."
 */
class ManaSeismScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(ManaSeism)
        d.initMirrorMatch(
            deck = Deck.of("Mountain" to 30, "Grizzly Bears" to 30),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.pool(p: EntityId): ManaPoolComponent =
        state.getEntity(p)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("sacrifices the chosen lands and adds that much colorless mana") {
        val d = newDriver()
        val p1 = d.player1

        val seism = d.putCardInHand(p1, "Mana Seism")
        val landA = d.putLandOnBattlefield(p1, "Mountain")
        val landB = d.putLandOnBattlefield(p1, "Mountain")
        val landC = d.putLandOnBattlefield(p1, "Mountain")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.giveMana(p1, Color.RED, 2)

        d.submit(CastSpell(playerId = p1, cardId = seism))
        d.bothPass()

        val sel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Only lands are offered.
        sel.options.contains(bear) shouldBe false
        sel.options.toSet() shouldBe setOf(landA, landB, landC)
        d.submitDecision(p1, CardsSelectedResponse(decisionId = sel.id, selectedCards = listOf(landA, landB)))

        d.isPaused shouldBe false
        d.getGraveyard(p1).contains(landA) shouldBe true
        d.getGraveyard(p1).contains(landB) shouldBe true
        d.getGraveyard(p1).contains(landC) shouldBe false
        d.pool(p1).colorless shouldBe 2
        d.pool(p1).red shouldBe 0
    }

    test("sacrificing zero lands adds no mana") {
        val d = newDriver()
        val p1 = d.player1

        val seism = d.putCardInHand(p1, "Mana Seism")
        val land = d.putLandOnBattlefield(p1, "Mountain")
        d.giveMana(p1, Color.RED, 2)

        d.submit(CastSpell(playerId = p1, cardId = seism))
        d.bothPass()

        val sel = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(p1, CardsSelectedResponse(decisionId = sel.id, selectedCards = emptyList()))

        d.isPaused shouldBe false
        d.getGraveyard(p1).contains(land) shouldBe false
        d.pool(p1).colorless shouldBe 0
    }
})
