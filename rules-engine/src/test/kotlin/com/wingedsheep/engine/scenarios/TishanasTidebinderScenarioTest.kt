package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.TishanasTidebinder
import com.wingedsheep.mtg.sets.definitions.scg.cards.CarrionFeeder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Tishana's Tidebinder (LCI).
 *
 * {2}{U} Creature — Merfolk Wizard, 3/2
 * Flash
 * When this creature enters, counter up to one target activated or triggered ability. If an ability
 * of an artifact, creature, or planeswalker is countered this way, that permanent loses all
 * abilities for as long as this creature remains on the battlefield. (Mana abilities can't be
 * targeted.)
 */
class TishanasTidebinderScenarioTest : FunSpec({

    val projector = StateProjector()

    // A creature with an ETB "gain 3 life" trigger, for the triggered-ability path.
    val LifeGainCreature = card("Life Gain Creature") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.GainLife(3)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TishanasTidebinder, LifeGainCreature))
        return driver
    }

    test("counters an activated ability and strips its creature source of all abilities") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), startingLife = 20)

        val caster = driver.activePlayer!!          // flashes Tishana in response
        val activator = driver.getOpponent(caster)  // activates Carrion Feeder
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activator, "Carrion Feeder")
        driver.removeSummoningSickness(feeder)
        val fodder = driver.putCreatureOnBattlefield(activator, "Grizzly Bears")

        val tishana = driver.putCardInHand(caster, "Tishana's Tidebinder")
        driver.giveMana(caster, Color.BLUE, 3) // {2}{U}

        // Hand priority to the activator, who activates Carrion Feeder (sacrifice Grizzly Bears).
        driver.passPriority(caster)
        val abilityId = CarrionFeeder.activatedAbilities[0].id
        driver.submit(
            ActivateAbility(
                playerId = activator,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        ).isSuccess shouldBe true
        val abilityOnStack = driver.getTopOfStack()!!

        // Activator passes; caster flashes Tishana in response.
        driver.passPriority(activator)
        driver.castSpell(caster, tishana)

        // Resolve Tishana; its ETB trigger fires and asks for a target.
        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(caster, listOf(abilityOnStack))

        // Resolve Tishana's ETB.
        driver.bothPass()

        // The activated ability was countered — no +1/+1 counter on Carrion Feeder.
        driver.stackSize shouldBe 0
        driver.state.getEntity(feeder)?.get<CountersComponent>() shouldBe null

        // Carrion Feeder loses all abilities while Tishana remains on the battlefield.
        projector.project(driver.state).hasLostAllAbilities(feeder) shouldBe true

        // The sacrificed creature is still gone (sacrifice was a cost, not part of the effect).
        driver.findPermanent(activator, "Grizzly Bears") shouldBe null
    }

    test("the ability-strip ends when Tishana's Tidebinder leaves the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), startingLife = 20)

        val caster = driver.activePlayer!!
        val activator = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activator, "Carrion Feeder")
        driver.removeSummoningSickness(feeder)
        val fodder = driver.putCreatureOnBattlefield(activator, "Grizzly Bears")

        val tishana = driver.putCardInHand(caster, "Tishana's Tidebinder")
        driver.giveMana(caster, Color.BLUE, 3)

        driver.passPriority(caster)
        val abilityId = CarrionFeeder.activatedAbilities[0].id
        driver.submit(
            ActivateAbility(
                playerId = activator,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        val abilityOnStack = driver.getTopOfStack()!!

        driver.passPriority(activator)
        driver.castSpell(caster, tishana)
        driver.bothPass()
        driver.submitTargetSelection(caster, listOf(abilityOnStack))
        driver.bothPass()

        // Stripped while Tishana is present.
        projector.project(driver.state).hasLostAllAbilities(feeder) shouldBe true

        // Tishana leaves the battlefield — the WhileSourceOnBattlefield strip ends.
        val tishanaPermanent = driver.findPermanent(caster, "Tishana's Tidebinder")!!
        driver.moveToGraveyard(tishanaPermanent)

        projector.project(driver.state).hasLostAllAbilities(feeder) shouldBe false
    }

    test("up to one: declining the target lets the ability resolve and strips nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), startingLife = 20)

        val caster = driver.activePlayer!!
        val activator = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val feeder = driver.putCreatureOnBattlefield(activator, "Carrion Feeder")
        driver.removeSummoningSickness(feeder)
        val fodder = driver.putCreatureOnBattlefield(activator, "Grizzly Bears")

        val tishana = driver.putCardInHand(caster, "Tishana's Tidebinder")
        driver.giveMana(caster, Color.BLUE, 3)

        driver.passPriority(caster)
        val abilityId = CarrionFeeder.activatedAbilities[0].id
        driver.submit(
            ActivateAbility(
                playerId = activator,
                sourceId = feeder,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )

        driver.passPriority(activator)
        driver.castSpell(caster, tishana)
        driver.bothPass()

        // "Up to one" — decline the target (choose none).
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(caster, emptyList())

        // Tishana's ETB resolves (declined, no-op)...
        driver.bothPass()
        // ...then the still-on-stack Carrion Feeder ability resolves normally.
        driver.bothPass()

        driver.stackSize shouldBe 0
        // The ability resolved: Carrion Feeder got its +1/+1 counter, abilities intact.
        driver.state.getEntity(feeder)?.get<CountersComponent>().shouldNotBeNull()
        projector.project(driver.state).hasLostAllAbilities(feeder) shouldBe false
    }

    test("counters a triggered ability and strips its creature source of all abilities") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20), startingLife = 20)

        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Caster casts Life Gain Creature (ETB: gain 3 life).
        val lifeGain = driver.putCardInHand(caster, "Life Gain Creature")
        driver.giveMana(caster, Color.GREEN, 2)
        driver.castSpell(caster, lifeGain)
        driver.bothPass() // creature resolves; ETB trigger goes on the stack

        driver.stackSize shouldBe 1
        val triggeredAbilityOnStack = driver.getTopOfStack()!!
        val lifeGainCreature = driver.findPermanent(caster, "Life Gain Creature")!!
        val lifeBefore = driver.getLifeTotal(caster)

        // Caster flashes Tishana in response to its own ETB trigger.
        val tishana = driver.putCardInHand(caster, "Tishana's Tidebinder")
        driver.giveMana(caster, Color.BLUE, 3)
        driver.castSpell(caster, tishana)

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(caster, listOf(triggeredAbilityOnStack))
        driver.bothPass()

        // The triggered ability was countered — no life gained.
        driver.stackSize shouldBe 0
        driver.getLifeTotal(caster) shouldBe lifeBefore

        // The Life Gain Creature loses all abilities while Tishana remains.
        projector.project(driver.state).hasLostAllAbilities(lifeGainCreature) shouldBe true
    }
})
