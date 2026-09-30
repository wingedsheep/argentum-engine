package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.NissaAscendedAnimist
import com.wingedsheep.mtg.sets.definitions.one.cards.SheoldredsEdict
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Sheoldred's Edict (ONE #108) — {1}{B} Instant.
 *
 * Choose one —
 * • Each opponent sacrifices a nontoken creature of their choice.
 * • Each opponent sacrifices a creature token of their choice.
 * • Each opponent sacrifices a planeswalker of their choice.
 */
class SheoldredsEdictScenarioTest : FunSpec({

    class Board(
        val driver: GameTestDriver,
        val you: EntityId,
        val opp: EntityId,
        val giant: EntityId,
        val token: EntityId,
        val walker: EntityId,
        val myBears: EntityId,
    )

    fun board(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SheoldredsEdict, NissaAscendedAnimist))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opp = driver.player2
        val giant = driver.putCreatureOnBattlefield(opp, "Hill Giant")
        val token = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.addComponent(token, TokenComponent)
        val walker = driver.putPermanentOnBattlefield(opp, "Nissa, Ascended Animist")
        driver.addComponent(walker, CountersComponent(mapOf(CounterType.LOYALTY to 7)))
        val myBears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        return Board(driver, you, opp, giant, token, walker, myBears)
    }

    fun Board.castMode(mode: Int) {
        val spell = driver.putCardInHand(you, "Sheoldred's Edict")
        driver.giveMana(you, Color.BLACK, 2)
        val result = driver.submit(
            CastSpell(playerId = you, cardId = spell, chosenModes = listOf(mode), modeTargetsOrdered = listOf(emptyList()))
        )
        if (result.outcome !is Outcome.Done) throw AssertionError("cast failed: ${result.error}")
        driver.bothPass()
    }

    fun Board.onBattlefield(id: EntityId): Boolean = driver.state.getBattlefield().contains(id)

    test("mode 1: opponent sacrifices their nontoken creature, keeping the token and planeswalker") {
        val b = board()
        b.castMode(0)
        b.onBattlefield(b.giant) shouldBe false
        b.driver.getGraveyardCardNames(b.opp) shouldContain "Hill Giant"
        b.onBattlefield(b.token) shouldBe true
        b.onBattlefield(b.walker) shouldBe true
        b.onBattlefield(b.myBears) shouldBe true
    }

    test("mode 2: opponent sacrifices their creature token, keeping the nontoken creature") {
        val b = board()
        b.castMode(1)
        b.onBattlefield(b.token) shouldBe false
        b.onBattlefield(b.giant) shouldBe true
        b.onBattlefield(b.walker) shouldBe true
        b.onBattlefield(b.myBears) shouldBe true
    }

    test("mode 3: opponent sacrifices their planeswalker, keeping their creatures") {
        val b = board()
        b.castMode(2)
        b.onBattlefield(b.walker) shouldBe false
        b.driver.getGraveyardCardNames(b.opp) shouldContain "Nissa, Ascended Animist"
        b.onBattlefield(b.giant) shouldBe true
        b.onBattlefield(b.token) shouldBe true
        b.onBattlefield(b.myBears) shouldBe true
    }

    test("token mode with no creature tokens does nothing — nontoken creatures are not eligible") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SheoldredsEdict))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        val b = Board(driver, driver.player1, driver.player2, EntityId.generate(), EntityId.generate(), EntityId.generate(), EntityId.generate())
        b.castMode(1)
        driver.findPermanent(driver.player2, "Hill Giant").shouldNotBeNull()
        driver.getGraveyardCardNames(driver.player2) shouldNotContain "Hill Giant"
        driver.state.pendingDecision.shouldBeNull()
    }
})
