package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thing in the Ice // Awoken Horror (SOI) — the ice-counter countdown.
 *
 * Covers the new `Counters.ICE` counter, the `Conditions.SourceCounterCountAtMost(ice, 0)` gate that
 * only flips the permanent on the resolution that removed the last counter, and the back face's
 * transforms-into trigger bouncing every non-Horror creature.
 */
class ThingInTheIceScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    /** Cast Thing in the Ice for real so the enters-with-counters replacement actually applies. */
    fun castThing(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Thing in the Ice")
        driver.giveMana(player, Color.BLUE, 2)
        driver.castSpell(player, card)
        driver.bothPass()
        return driver.findPermanent(player, "Thing in the Ice")!!
    }

    /** Cast a Lightning Bolt at the opponent and let the cast trigger + the bolt both resolve. */
    fun boltOpponent(driver: GameTestDriver, player: EntityId) {
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpellWithTargets(
            player, bolt, listOf(ChosenTarget.Player(driver.getOpponent(player)))
        )
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    fun iceCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.ICE) ?: 0

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    test("enters with four ice counters and has defender") {
        val (driver, you) = newGame()
        val thing = castThing(driver, you)

        iceCounters(driver, thing) shouldBe 4
        val projected = projector.project(driver.state)
        projected.hasKeyword(thing, Keyword.DEFENDER) shouldBe true
        projected.getPower(thing) shouldBe 0
        projected.getToughness(thing) shouldBe 4
    }

    test("each instant or sorcery you cast removes one ice counter, without flipping early") {
        val (driver, you) = newGame()
        val thing = castThing(driver, you)

        repeat(3) { boltOpponent(driver, you) }

        iceCounters(driver, thing) shouldBe 1
        faceName(driver, thing) shouldBe "Thing in the Ice"
    }

    test("removing the last ice counter transforms it and bounces all non-Horror creatures") {
        val (driver, you) = newGame()
        val opponent = driver.getOpponent(you)
        val thing = castThing(driver, you)

        // A non-Horror creature on each side — both should end up back in their owners' hands.
        driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val handBefore = driver.getHandSize(you)
        val opponentHandBefore = driver.getHandSize(opponent)

        repeat(4) { boltOpponent(driver, you) }

        iceCounters(driver, thing) shouldBe 0
        faceName(driver, thing) shouldBe "Awoken Horror"
        projector.project(driver.state).getPower(thing) shouldBe 7
        projector.project(driver.state).getToughness(thing) shouldBe 8

        // Awoken Horror is a Horror, so it stays; both Coursers went home.
        driver.findPermanent(you, "Centaur Courser") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getHandSize(you) shouldBe handBefore + 1
        driver.getHandSize(opponent) shouldBe opponentHandBefore + 1
        driver.findPermanent(you, "Awoken Horror") shouldBe thing
    }

    test("removing the last ice counter another way does not transform it (printed ruling)") {
        val (driver, you) = newGame()
        val thing = castThing(driver, you)

        // Strip every ice counter directly — no cast trigger resolves, so nothing flips.
        val container = driver.state.getEntity(thing)!!
        driver.replaceState(
            driver.state.withEntity(
                thing,
                container.with(CountersComponent(emptyMap()))
            )
        )

        iceCounters(driver, thing) shouldBe 0
        faceName(driver, thing) shouldBe "Thing in the Ice"
    }
})
