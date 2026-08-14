package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.ThaliaHereticCathar
import com.wingedsheep.mtg.sets.definitions.ice.cards.AdarkarWastes
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thalia, Heretic Cathar (EMN #46) — {2}{W} 3/2 Legendary Creature — Human Soldier
 *
 * "First strike
 *  Creatures and nonbasic lands your opponents control enter tapped."
 *
 * Two independent `PermanentsEnterTapped` replacements, so the test covers both halves plus the
 * cases they must *not* touch: your own permanents, and your opponents' basic lands.
 */
class ThaliaHereticCatharScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThaliaHereticCathar, bear, AdarkarWastes))
        return driver
    }

    /** Cast [cardName] off injected mana and let it resolve. */
    fun castAndResolve(driver: GameTestDriver, playerId: EntityId, cardName: String): EntityId {
        val cardId = driver.putCardInHand(playerId, cardName)
        driver.giveColorlessMana(playerId, 1)
        driver.submitSuccess(
            CastSpell(playerId = playerId, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()
        return cardId
    }

    /** Advance turns until [playerId] is the active player, then stop in their precombat main. */
    fun handTurnTo(driver: GameTestDriver, playerId: EntityId) {
        while (driver.activePlayer != playerId) {
            driver.passPriorityUntil(Step.END)
            driver.bothPass()
        }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("an opponent's creature enters tapped; your own does not") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Thalia, Heretic Cathar")

        // Yours is unaffected.
        val yourBear = castAndResolve(driver, you, "Test Bear")
        driver.isTapped(yourBear) shouldBe false

        // Theirs enters tapped.
        handTurnTo(driver, opponent)
        val theirBear = castAndResolve(driver, opponent, "Test Bear")
        driver.isTapped(theirBear) shouldBe true
    }

    test("an opponent's nonbasic land enters tapped but a basic land does not") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 30, "Adarkar Wastes" to 10),
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Thalia, Heretic Cathar")

        handTurnTo(driver, opponent)

        // Nonbasic land: tapped.
        val nonbasic = driver.putCardInHand(opponent, "Adarkar Wastes")
        driver.submitSuccess(com.wingedsheep.engine.core.PlayLand(playerId = opponent, cardId = nonbasic))
        driver.isTapped(nonbasic) shouldBe true

        // Basic land on the next turn: untapped — the filter is nonbasic lands only.
        handTurnTo(driver, you)
        handTurnTo(driver, opponent)
        val basic = driver.putCardInHand(opponent, "Plains")
        driver.submitSuccess(com.wingedsheep.engine.core.PlayLand(playerId = opponent, cardId = basic))
        driver.isTapped(basic) shouldBe false
    }
})
