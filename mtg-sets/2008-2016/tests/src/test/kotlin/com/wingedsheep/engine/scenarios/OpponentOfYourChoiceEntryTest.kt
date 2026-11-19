package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.FanaticOfXenagos
import com.wingedsheep.mtg.sets.definitions.ody.cards.Zombify
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "An opponent of your choice" as a permanent enters — an `EntersWithChoice` whose chooser is
 * `Player.AnOpponent` (Tribute, CR 702.104a: "As this creature enters, choose an opponent. That
 * player may put an additional N +1/+1 counters on it"). With several opponents the controller
 * picks one first; that opponent answers, and is the one who puts the counters on. Covered on the
 * spell path (cast) and the effect-driven path (reanimated by Zombify), with Fanatic of Xenagos.
 */
class OpponentOfYourChoiceEntryTest : FunSpec({

    fun threePlayerGame(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FanaticOfXenagos + Zombify)
        val players = driver.initMultiplayer(decks = List(3) { Deck.of("Forest" to 20, "Swamp" to 20) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    fun GameTestDriver.resolveTopOfStack() = repeat(state.turnOrder.size) {
        passPriority(state.priorityPlayerId!!)
    }

    /** The controller picks [chosen]; [chosen] then pays tribute. */
    fun GameTestDriver.pickOpponentAndPay(controller: EntityId, chosen: EntityId) {
        val pick = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe controller
        pick.options.size shouldBe 2
        submitDecision(controller, OptionChosenResponse(pick.id, state.getOpponents(controller).indexOf(chosen)))

        val tribute = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        tribute.playerId shouldBe chosen
        submitDecision(chosen, OptionChosenResponse(tribute.id, tribute.options.indexOfFirst { it.startsWith("Pay tribute") }))
    }

    fun GameTestDriver.assertTributePaidBy(fanatic: EntityId, payer: EntityId) {
        state.getEntity(fanatic)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        events.filterIsInstance<CountersAddedEvent>()
            .single { it.entityId == fanatic && it.counterType == CounterType.PLUS_ONE_PLUS_ONE }
            .placedBy shouldBe payer
    }

    test("cast: the controller chooses the second opponent, who pays tribute and places the counter") {
        val (driver, players) = threePlayerGame()
        val me = players[0]
        val second = players[2]
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Fanatic of Xenagos")).outcome shouldBe Outcome.Done
        driver.resolveTopOfStack()

        driver.pickOpponentAndPay(me, second)

        driver.assertTributePaidBy(driver.findPermanent(me, "Fanatic of Xenagos")!!, second)
    }

    test("reanimated: the effect-driven entry asks the same two questions") {
        val (driver, players) = threePlayerGame()
        val me = players[0]
        val second = players[2]
        val fanatic = driver.putCardInGraveyard(me, "Fanatic of Xenagos")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpellWithTargets(
            me, driver.putCardInHand(me, "Zombify"), listOf(ChosenTarget.Card(fanatic, me, Zone.GRAVEYARD))
        ).outcome shouldBe Outcome.Done
        driver.resolveTopOfStack()

        driver.pickOpponentAndPay(me, second)

        driver.assertTributePaidBy(driver.findPermanent(me, "Fanatic of Xenagos")!!, second)
    }

    test("two players: no opponent pick, the only opponent is asked directly") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FanaticOfXenagos)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Fanatic of Xenagos")).outcome shouldBe Outcome.Done
        driver.bothPass()

        val tribute = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        tribute.playerId shouldBe opponent
        driver.submitDecision(opponent, OptionChosenResponse(tribute.id, tribute.options.indexOfFirst { it.startsWith("Pay tribute") }))

        driver.assertTributePaidBy(driver.findPermanent(me, "Fanatic of Xenagos")!!, opponent)
    }
})
