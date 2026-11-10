package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.BearerOfSilence
import com.wingedsheep.mtg.sets.definitions.ogw.cards.CultivatorDrone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cultivator Drone — "{T}: Add {C}. Spend this mana only to cast a colorless spell, activate an
 * ability of a colorless permanent, or pay a cost that contains {C}."
 *
 * Each test taps the Drone into its controller's pool and then spends only that restricted {C}.
 */
class CultivatorDroneScenarioTest : FunSpec({

    val colorlessTrinket = CardDefinition.artifact(name = "Drone Test Trinket", manaCost = ManaCost.parse("{1}"))
    val coloredOneDrop = card("Drone Test Colored Spell") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }
    val coloredCSpell = card("Drone Test {C} Spell") {
        manaCost = "{U}{C}"
        typeLine = "Sorcery"
        oracleText = "You gain 2 life."
        spell { effect = Effects.GainLife(2) }
    }
    val colorlessEngine = card("Drone Test Colorless Engine") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
            timing = TimingRule.InstantSpeed
        }
    }
    val coloredEngine = card("Drone Test Colored Engine") {
        manaCost = "{U}"
        typeLine = "Enchantment"
        oracleText = "{1}: You gain 1 life.\n{C}: You gain 3 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
            timing = TimingRule.InstantSpeed
        }
        activatedAbility {
            cost = Costs.Mana("{C}")
            effect = Effects.GainLife(3)
            timing = TimingRule.InstantSpeed
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(CultivatorDrone, BearerOfSilence, colorlessTrinket, coloredOneDrop, coloredCSpell, colorlessEngine, coloredEngine)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.restrictedCount(playerId: EntityId) =
        state.getEntity(playerId)!!.get<ManaPoolComponent>()!!.restrictedMana.size

    /** Put a Drone onto the battlefield and tap it for its restricted {C}. */
    fun GameTestDriver.tapDrone(playerId: EntityId) {
        val drone = putCreatureOnBattlefield(playerId, "Cultivator Drone")
        removeSummoningSickness(drone)
        submitSuccess(ActivateAbility(playerId, drone, CultivatorDrone.activatedAbilities.single().id))
        restrictedCount(playerId) shouldBe 1
    }

    test("the mana casts a colorless spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val trinket = driver.putCardInHand(me, "Drone Test Trinket")
        driver.tapDrone(me)

        val result = driver.submit(CastSpell(me, trinket, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.restrictedCount(me) shouldBe 0
    }

    test("the mana pays the generic part of a devoid spell, which is colorless") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bearer = driver.putCardInHand(me, "Bearer of Silence")
        driver.giveMana(me, Color.BLACK, 1)
        driver.tapDrone(me)

        val result = driver.submit(CastSpell(me, bearer, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.error shouldBe null }
        driver.restrictedCount(me) shouldBe 0
    }

    test("the mana can't pay the generic part of a colored spell without {C} in its cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Drone Test Colored Spell")
        driver.giveMana(me, Color.BLUE, 1)
        driver.tapDrone(me)

        driver.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldNotBe Outcome.Done
        driver.restrictedCount(me) shouldBe 1
    }

    test("the mana pays a colored spell whose cost contains {C}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Drone Test {C} Spell")
        driver.giveMana(me, Color.BLUE, 1)
        driver.tapDrone(me)

        val result = driver.submit(CastSpell(me, spell, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.restrictedCount(me) shouldBe 0
    }

    test("the mana activates an ability of a colorless permanent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val engine = driver.putPermanentOnBattlefield(me, "Drone Test Colorless Engine")
        driver.tapDrone(me)
        val lifeBefore = driver.getLifeTotal(me)

        driver.submitSuccess(ActivateAbility(me, engine, colorlessEngine.activatedAbilities.single().id))
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 1
    }

    test("on a colored permanent the mana pays only an ability whose cost contains {C}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val engine = driver.putPermanentOnBattlefield(me, "Drone Test Colored Engine")
        driver.tapDrone(me)
        val lifeBefore = driver.getLifeTotal(me)
        val (genericAbility, cAbility) = coloredEngine.activatedAbilities.map { it.id }

        driver.submit(ActivateAbility(me, engine, genericAbility)).outcome shouldNotBe Outcome.Done
        driver.submitSuccess(ActivateAbility(me, engine, cAbility))
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe lifeBefore + 3
    }
})
