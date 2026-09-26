package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FlippedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.NezumiShortfang
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Nezumi Shortfang // Stabwhisker the Odious (CHK) — a flip card.
 *
 * "{1}{B}, {T}: Target opponent discards a card. Then if that player has no cards in hand, flip
 * this creature."
 * Stabwhisker: "At the beginning of each opponent's upkeep, that player loses 1 life for each card
 * fewer than three in their hand."
 */
class NezumiShortfangScenarioTest : FunSpec({

    val discardAbility = NezumiShortfang.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NezumiShortfang)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.reduceHandTo(playerId: EntityId, count: Int) {
        val hand = getHand(playerId)
        if (hand.size <= count) return
        val handKey = ZoneKey(playerId, Zone.HAND)
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val newZones = state.zones.toMutableMap()
        newZones[handKey] = hand.take(count)
        newZones[libraryKey] = hand.drop(count) + (newZones[libraryKey] ?: emptyList())
        replaceState(state.copy(zones = newZones))
    }

    fun GameTestDriver.activateDiscard(nezumi: EntityId) {
        giveMana(player1, Color.BLACK, 2)
        submitSuccess(
            ActivateAbility(
                player1, nezumi, discardAbility,
                targets = listOf(ChosenTarget.Player(player2)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        bothPass()
        // The opponent picks the card to discard when there is a choice to make.
        if (pendingDecision != null) {
            submitCardSelection(player2, listOf(getHand(player2).first()))
        }
    }

    fun GameTestDriver.name(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!.name

    fun GameTestDriver.nezumiOnBattlefield(): EntityId {
        val nezumi = putCreatureOnBattlefield(player1, "Nezumi Shortfang")
        removeSummoningSickness(nezumi)
        return nezumi
    }

    test("opponent keeping a card in hand leaves it unflipped") {
        val d = driver()
        val nezumi = d.nezumiOnBattlefield()
        d.reduceHandTo(d.player2, 2)

        d.activateDiscard(nezumi)

        d.getHandSize(d.player2) shouldBe 1
        d.name(nezumi) shouldBe "Nezumi Shortfang"
        d.state.getEntity(nezumi)!!.get<FlippedComponent>().shouldBeNull()
    }

    test("discarding the opponent's last card flips it into Stabwhisker the Odious") {
        val d = driver()
        val nezumi = d.nezumiOnBattlefield()
        d.reduceHandTo(d.player2, 1)

        d.activateDiscard(nezumi)

        d.getHandSize(d.player2) shouldBe 0
        d.name(nezumi) shouldBe "Stabwhisker the Odious"
        d.state.getEntity(nezumi)!!.get<FlippedComponent>().shouldNotBeNull()
        withClue("legendary 3/3, still black") {
            d.state.projectedState.getPower(nezumi) shouldBe 3
            d.state.projectedState.getToughness(nezumi) shouldBe 3
            d.state.projectedState.isLegendary(nezumi) shouldBe true
            d.state.projectedState.getColors(nezumi) shouldBe setOf(Color.BLACK.name)
        }
    }

    test("an opponent with an already empty hand still flips it") {
        val d = driver()
        val nezumi = d.nezumiOnBattlefield()
        d.reduceHandTo(d.player2, 0)

        d.activateDiscard(nezumi)

        d.name(nezumi) shouldBe "Stabwhisker the Odious"
    }

    fun flipAndGoToOpponentUpkeep(opponentHandAtUpkeep: Int): Pair<GameTestDriver, Int> {
        val d = driver()
        val nezumi = d.nezumiOnBattlefield()
        d.reduceHandTo(d.player2, 0)
        d.activateDiscard(nezumi)
        d.name(nezumi) shouldBe "Stabwhisker the Odious"
        repeat(opponentHandAtUpkeep) { d.putCardInHand(d.player2, "Swamp") }
        val lifeBefore = d.getLifeTotal(d.player2)

        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.currentStep shouldBe Step.UPKEEP
        d.activePlayer shouldBe d.player2
        return d to lifeBefore
    }

    test("Stabwhisker drains 3 from an opponent with an empty hand at upkeep") {
        val (d, lifeBefore) = flipAndGoToOpponentUpkeep(0)
        d.stackSize shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe lifeBefore - 3
    }

    test("Stabwhisker drains 1 per card fewer than three") {
        val (d, lifeBefore) = flipAndGoToOpponentUpkeep(2)
        d.stackSize shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe lifeBefore - 1
    }

    test("Stabwhisker drains nothing from an opponent holding more than three cards") {
        val (d, lifeBefore) = flipAndGoToOpponentUpkeep(5)
        if (d.stackSize > 0) d.bothPass()
        d.getLifeTotal(d.player2) shouldBe lifeBefore
    }

    test("Stabwhisker does not trigger on its controller's upkeep") {
        val (d, _) = flipAndGoToOpponentUpkeep(5)
        if (d.stackSize > 0) d.bothPass()
        val controllerLife = d.getLifeTotal(d.player1)
        d.reduceHandTo(d.player1, 0)
        d.passPriorityUntil(Step.DRAW, maxPasses = 200)
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.activePlayer shouldBe d.player1
        d.stackSize shouldBe 0
        d.getLifeTotal(d.player1) shouldBe controllerLife
    }
})
