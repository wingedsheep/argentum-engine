package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.JinGitaxias
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jin-Gitaxias // The Great Synthesis (MOM #65).
 *
 * Front: ward {2}; "whenever you cast a noncreature spell with mana value 3 or greater, draw a card";
 * {3}{U} sorcery-speed transform gated on seven or more cards in hand.
 * Back (Saga): I — draw cards equal to your hand size, no maximum hand size while you control it;
 * II — bounce every non-Phyrexian creature; III — cast any number of spells from hand for free,
 * then exile the Saga and return it as Jin-Gitaxias before any of those spells resolve.
 */
class JinGitaxiasScenarioTest : FunSpec({

    val bigSorcery = card("Synthesis Test Sorcery") {
        manaCost = "{3}{U}"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell { effect = Effects.GainLife(3) }
    }
    val smallSorcery = card("Synthesis Test Cantrip") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val bigCreature = card("Synthesis Test Giant") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Giant"
        power = 3
        toughness = 3
    }
    val phyrexian = card("Synthesis Test Phyrexian") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Phyrexian Horror"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(JinGitaxias, bigSorcery, smallSorcery, bigCreature, phyrexian)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String =
        driver.state.getEntity(id)!!.get<CardComponent>()!!.name

    fun lore(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun castFree(driver: GameTestDriver, player: EntityId, name: String) {
        val spell = driver.putCardInHand(player, name)
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 3)
        driver.castSpell(player, spell).outcome shouldBe Outcome.Done
        resolveStack(driver)
    }

    fun activateTransform(driver: GameTestDriver, you: EntityId, jin: EntityId): Outcome {
        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 3)
        return driver.submit(
            ActivateAbility(playerId = you, sourceId = jin, abilityId = JinGitaxias.activatedAbilities.first().id)
        ).outcome
    }

    /** Jin on the battlefield with a seven-card hand, flipped into The Great Synthesis (chapter I resolved). */
    fun transformIntoSaga(driver: GameTestDriver, you: EntityId): EntityId {
        val jin = driver.putCreatureOnBattlefield(you, "Jin-Gitaxias")
        driver.getHand(you).size shouldBe 7
        activateTransform(driver, you, jin) shouldBe Outcome.Done
        resolveStack(driver)
        return jin
    }

    test("casting a noncreature spell with mana value 3 or greater draws a card; smaller or creature spells don't") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Jin-Gitaxias")

        val handBefore = driver.getHand(you).size
        castFree(driver, you, "Synthesis Test Sorcery")
        withClue("MV 4 sorcery: put in hand, cast, drew one") {
            driver.getHand(you).size shouldBe handBefore + 1
        }

        castFree(driver, you, "Synthesis Test Cantrip")
        withClue("MV 2 sorcery draws nothing") { driver.getHand(you).size shouldBe handBefore + 1 }

        castFree(driver, you, "Synthesis Test Giant")
        withClue("a creature spell draws nothing") { driver.getHand(you).size shouldBe handBefore + 1 }
    }

    test("transform needs seven cards in hand; chapter I doubles the hand and lifts the maximum hand size") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val jin = driver.putCreatureOnBattlefield(you, "Jin-Gitaxias")

        val land = driver.getHand(you).first()
        driver.playLand(you, land)
        driver.getHand(you).size shouldBe 6
        withClue("six cards in hand: the ability can't be activated") {
            activateTransform(driver, you, jin) shouldNotBe Outcome.Done
        }

        driver.putCardInHand(you, "Island")
        activateTransform(driver, you, jin) shouldBe Outcome.Done
        resolveStack(driver)

        nameOf(driver, jin) shouldBe "The Great Synthesis"
        lore(driver, jin) shouldBe 1
        withClue("chapter I drew seven (the hand size on resolution)") {
            driver.getHand(you).size shouldBe 14
        }

        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 300) // through your cleanup into the opponent's turn
        withClue("no maximum hand size while you control the Saga — nothing discarded at cleanup") {
            driver.getHand(you).size shouldBe 14
        }
    }

    test("chapter II returns all non-Phyrexian creatures to their owners' hands") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val saga = transformIntoSaga(driver, you)

        val yourBear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val phyrexianHorror = driver.putCreatureOnBattlefield(opp, "Synthesis Test Phyrexian")

        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300) // opponent's turn
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300) // your turn: lore 2
        resolveStack(driver)

        lore(driver, saga) shouldBe 2
        driver.getHand(you) shouldContain yourBear
        driver.getHand(opp) shouldContain theirBear
        withClue("the Phyrexian creature stays") {
            driver.state.getBattlefield() shouldContain phyrexianHorror
        }
    }

    test("chapter III casts any number of spells free, then returns as Jin-Gitaxias before they resolve") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val saga = transformIntoSaga(driver, you)

        repeat(3) { // opponent's turn 2, your turn 3 (chapter II), opponent's turn 4
            driver.passPriorityUntil(Step.END, maxPasses = 300)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
            resolveStack(driver)
        }
        driver.passPriorityUntil(Step.END, maxPasses = 300) // opponent's turn 4 end step

        val sorcery = driver.putCardInHand(you, "Synthesis Test Sorcery")
        val giant = driver.putCardInHand(you, "Synthesis Test Giant")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300) // your turn 5: chapter III on the stack
        lore(driver, saga) shouldBe 3
        driver.bothPass()

        val lifeBefore = driver.getLifeTotal(you)
        val first = driver.pendingDecision as SelectCardsDecision
        withClue("only nonland cards in hand are offered") {
            first.options.toSet() shouldBe setOf(sorcery, giant)
        }
        driver.submitCardSelection(you, listOf(sorcery))
        val second = driver.pendingDecision as SelectCardsDecision
        second.options shouldBe listOf(giant)
        driver.submitCardSelection(you, listOf(giant))
        // Hand now holds no nonland card; the loop ends (prompting again only to decline, if at all).
        (driver.pendingDecision as? SelectCardsDecision)?.let { driver.submitCardSelection(you, emptyList()) }

        withClue("the Saga has already returned as Jin-Gitaxias while both spells wait on the stack") {
            nameOf(driver, saga) shouldBe "Jin-Gitaxias"
            driver.state.getBattlefield() shouldContain saga
            driver.state.stack.size shouldBe 2
        }

        resolveStack(driver)
        driver.getLifeTotal(you) shouldBe lifeBefore + 3
        driver.findPermanent(you, "Synthesis Test Giant") shouldNotBe null
        driver.getHand(you) shouldNotContain sorcery
    }
})
