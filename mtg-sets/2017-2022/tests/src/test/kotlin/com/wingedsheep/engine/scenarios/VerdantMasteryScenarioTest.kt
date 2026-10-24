package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.VerdantMastery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Verdant Mastery (STX #146): "You may pay {3}{G} rather than pay this spell's mana cost. Search
 * your library for up to four basic land cards and reveal them. Put one of them onto the
 * battlefield tapped under an opponent's control if the {3}{G} cost was paid. Put two of them onto
 * the battlefield tapped under your control and the rest into your hand. Then shuffle."
 */
class VerdantMasteryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + VerdantMastery)
        it.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.select(player: EntityId, max: Int, pick: (List<EntityId>) -> List<EntityId>): List<EntityId> {
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe player
        decision.maxSelections shouldBe max
        val chosen = pick(decision.options)
        submitCardSelection(player, chosen).error shouldBe null
        return chosen
    }

    test("cast for {3}{G}: one land enters tapped under the opponent's control, two under yours, one to hand") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val mastery = d.putCardInHand(me, "Verdant Mastery")
        d.giveMana(me, Color.GREEN, 4)
        val myHand = d.getHand(me).size
        val myLands = d.getLands(me).size
        val opponentLands = d.getLands(opponent).size

        d.submit(CastSpell(me, mastery, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)).error shouldBe null
        d.bothPass()

        val found = d.select(me, 4) { it.take(4) }
        val gift = d.select(me, 1) { it.take(1) }.single()
        val mine = d.select(me, 2) { options ->
            (gift in options) shouldBe false
            options.take(2)
        }
        val toHand = found - gift - mine.toSet()

        d.pendingDecision shouldBe null
        d.getController(gift) shouldBe opponent
        d.state.getEntity(gift)?.get<OwnerComponent>()?.playerId shouldBe me
        d.isTapped(gift) shouldBe true
        d.getLands(opponent).size shouldBe opponentLands + 1
        mine.forEach {
            d.getController(it) shouldBe me
            d.isTapped(it) shouldBe true
        }
        d.getLands(me).size shouldBe myLands + 2
        d.getHand(me).toSet().containsAll(toHand) shouldBe true
        d.getHand(me).size shouldBe myHand - 1 + 1
    }

    test("cast for {5}{G}: two lands enter tapped under your control, two go to hand, the opponent gets none") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val mastery = d.putCardInHand(me, "Verdant Mastery")
        d.giveMana(me, Color.GREEN, 6)
        val myHand = d.getHand(me).size
        val myLands = d.getLands(me).size
        val opponentLands = d.getLands(opponent).size

        d.castSpell(me, mastery).error shouldBe null
        d.bothPass()

        val found = d.select(me, 4) { it.take(4) }
        val mine = d.select(me, 2) { it.take(2) }

        d.pendingDecision shouldBe null
        mine.forEach {
            d.getController(it) shouldBe me
            d.isTapped(it) shouldBe true
        }
        d.getLands(me).size shouldBe myLands + 2
        d.getLands(opponent).size shouldBe opponentLands
        d.getHand(me).toSet().containsAll(found - mine.toSet()) shouldBe true
        d.getHand(me).size shouldBe myHand - 1 + 2
    }
})
