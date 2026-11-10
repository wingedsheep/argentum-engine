package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.CorruptedCrossroads
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Corrupted Crossroads — "{T}: Add {C}. {T}, Pay 1 life: Add one mana of any color. Spend this
 * mana only to cast a spell with devoid."
 *
 * The colored mana keys on the devoid keyword, not on colorlessness: a colorless artifact without
 * devoid can't spend it (ruling), and neither can an ordinary colored spell.
 */
class CorruptedCrossroadsScenarioTest : FunSpec({

    val devoidSpell = card("Crossroads Test Devoid Spell") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Devoid\nYou gain 1 life."
        keywords(Keyword.DEVOID)
        spell { effect = Effects.GainLife(1) }
    }
    val coloredSpell = card("Crossroads Test Colored Spell") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val colorlessTrinket = CardDefinition.artifact(name = "Crossroads Test Trinket", manaCost = ManaCost.parse("{1}"))

    val colorlessAbility = CorruptedCrossroads.activatedAbilities[0].id
    val coloredAbility = CorruptedCrossroads.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CorruptedCrossroads, devoidSpell, coloredSpell, colorlessTrinket))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.pool(playerId: EntityId) = state.getEntity(playerId)!!.get<ManaPoolComponent>()!!

    /** Put a Crossroads onto the battlefield and tap it, paying 1 life, for restricted black mana. */
    fun GameTestDriver.tapForBlack(playerId: EntityId) {
        val land = putPermanentOnBattlefield(playerId, "Corrupted Crossroads")
        submitSuccess(ActivateAbility(playerId, land, coloredAbility, manaColorChoice = Color.BLACK))
        getLifeTotal(playerId) shouldBe 19
        pool(playerId).restrictedMana.size shouldBe 1
    }

    test("{T} adds {C}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val land = driver.putPermanentOnBattlefield(me, "Corrupted Crossroads")
        driver.submitSuccess(ActivateAbility(me, land, colorlessAbility))
        driver.pool(me).colorless shouldBe 1
        driver.getLifeTotal(me) shouldBe 20
    }

    test("the paid-for colored mana casts a spell with devoid") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Crossroads Test Devoid Spell")
        driver.tapForBlack(me)

        val result = driver.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.pool(me).restrictedMana.size shouldBe 0
    }

    test("the colored mana can't cast a colorless spell without devoid") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val trinket = driver.putCardInHand(me, "Crossroads Test Trinket")
        driver.tapForBlack(me)

        driver.submit(CastSpell(me, trinket, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldNotBe Outcome.Done
        driver.pool(me).restrictedMana.size shouldBe 1
    }

    test("the colored mana can't cast a colored spell without devoid") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Crossroads Test Colored Spell")
        driver.tapForBlack(me)

        driver.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldNotBe Outcome.Done
        driver.pool(me).restrictedMana.size shouldBe 1
    }
})
