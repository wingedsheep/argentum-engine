package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmm.cards.NarciFableSinger
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Narci, Fable Singer — "Whenever the final chapter ability of a Saga you control resolves, each
 * opponent loses X life and you gain X life, where X is that Saga's mana value." The Saga is
 * sacrificed (CR 714.4) before Narci's trigger resolves, so X is read off its last-known
 * information — and that sacrifice is itself "you sacrifice an enchantment", drawing a card.
 */
class NarciFableSingerScenarioTest : FunSpec({

    // Mana value 3, one chapter: casting it resolves its final chapter at once.
    val OneChapterSaga = card("Three-Drop One-Chapter Saga") {
        manaCost = "{3}"
        typeLine = "Enchantment — Saga"
        oracleText = "I — You gain 1 life."
        sagaChapter(1) { effect = Effects.GainLife(1) }
    }

    val TwoChapterSaga = card("Two-Chapter Saga") {
        manaCost = "{5}"
        typeLine = "Enchantment — Saga"
        oracleText = "I, II — You gain 1 life."
        sagaChapter(1) { effect = Effects.GainLife(1) }
        sagaChapter(2) { effect = Effects.GainLife(1) }
    }

    val Trinket = card("Sacrificial Trinket") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "Sacrifice this artifact: You gain 1 life."
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.GainLife(1)
        }
    }

    val Charm = card("Sacrificial Charm") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Sacrifice this enchantment: You gain 1 life."
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.GainLife(1)
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NarciFableSinger, OneChapterSaga, TwoChapterSaga, Trinket, Charm))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castAndResolve(name: String, mana: Int) {
        val player = activePlayer!!
        val spell = putCardInHand(player, name)
        giveColorlessMana(player, mana)
        castSpell(player, spell)
        var guard = 0
        do { bothPass() } while (guard++ < 20 && state.stack.isNotEmpty())
    }

    fun GameTestDriver.onBattlefield(name: String): Boolean =
        state.getBattlefield().any { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("final chapter drains each opponent for the sacrificed Saga's mana value and draws a card") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Narci, Fable Singer")
        val handBefore = driver.getHandSize(me)

        driver.castAndResolve("Three-Drop One-Chapter Saga", 3)

        driver.onBattlefield("Three-Drop One-Chapter Saga") shouldBe false
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(me) shouldBe 24 // chapter I's 1 + Narci's 3
        driver.getHandSize(me) shouldBe handBefore + 1 // the CR 714.4 sacrifice is a sacrifice
    }

    test("a token Saga that has ceased to exist still drains for its last-known mana value") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Narci, Fable Singer")
        val saga = driver.putPermanentOnBattlefield(me, "Three-Drop One-Chapter Saga")
        driver.replaceState(driver.state.updateEntity(saga) { it.with(TokenComponent).with(SagaComponent()) })

        // Our next precombat main: the lore counter triggers chapter I, its final chapter.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        val handBefore = driver.getHandSize(me)
        var guard = 0
        while (guard++ < 20 && driver.state.stack.isNotEmpty()) driver.bothPass()

        // The sacrificed token is gone from every zone (CR 704.5d) before Narci's trigger resolves.
        (saga in driver.state.getBattlefield()) shouldBe false
        (saga in driver.getGraveyard(me)) shouldBe false
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(me) shouldBe 24
        driver.getHandSize(me) shouldBe handBefore + 1 // the token was still sacrificed
    }

    test("only an enchantment sacrifice draws a card") {
        val driver = setup()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Narci, Fable Singer")
        val trinket = driver.putPermanentOnBattlefield(me, "Sacrificial Trinket")
        val charm = driver.putPermanentOnBattlefield(me, "Sacrificial Charm")
        val handBefore = driver.getHandSize(me)

        driver.submit(ActivateAbility(me, trinket, Trinket.activatedAbilities.first().id))
        driver.bothPass()
        driver.getHandSize(me) shouldBe handBefore

        driver.submit(ActivateAbility(me, charm, Charm.activatedAbilities.first().id))
        var guard = 0
        do { driver.bothPass() } while (guard++ < 20 && driver.state.stack.isNotEmpty())
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("a chapter that isn't the final one doesn't drain") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Narci, Fable Singer")
        val handBefore = driver.getHandSize(me)

        driver.castAndResolve("Two-Chapter Saga", 5)

        driver.onBattlefield("Two-Chapter Saga") shouldBe true
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(me) shouldBe 21
        driver.getHandSize(me) shouldBe handBefore
    }
})
