package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.CAMP
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * C.A.M.P. — "Whenever fortified land is tapped for mana, put a +1/+1 counter on target creature you
 * control. If that creature shares a color with the mana that land produced, create a Junk token.
 * Fortify {3}"
 *
 * Exercises Fortify through a real card and the tapped-for-mana trigger on the ATTACHED binding,
 * whose resolving condition reads the colors the land produced.
 */
class CAMPScenarioTest : FunSpec({

    val wastes = card("CAMP Test Wastes") {
        typeLine = "Land"
        activatedAbility {
            manaAbility = true
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
        }
    }
    val greenTwoDrop = card("CAMP Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val greenCycler = card("CAMP Test Cycler") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.cycling("{G}"))
    }

    // Two mana-producing effects in one ability (Overflowing Basin's {G}{U} shape).
    val simicLand = card("CAMP Test Simic Land") {
        typeLine = "Land"
        activatedAbility {
            manaAbility = true
            cost = Costs.Tap
            effect = Effects.Composite(listOf(Effects.AddMana(Color.GREEN), Effects.AddMana(Color.BLUE)))
        }
    }
    val blueOneDrop = card("CAMP Test Merfolk") {
        manaCost = "{U}"
        typeLine = "Creature — Merfolk"
        power = 1
        toughness = 1
    }

    val fortifyId = CAMP.activatedAbilities.single { it.isFortifyAbility }.id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CAMP, PredefinedTokens.Junk, wastes, greenTwoDrop, greenCycler, simicLand, blueOneDrop))
        initMirrorMatch(deck = Deck.of("Island" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.fortify(you: EntityId, camp: EntityId, land: EntityId) {
        giveColorlessMana(you, 3)
        submit(
            ActivateAbility(
                playerId = you, sourceId = camp, abilityId = fortifyId,
                targets = listOf(ChosenTarget.Permanent(land)), paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        bothPass()
        state.getEntity(camp)!!.get<AttachedToComponent>()?.targetId shouldBe land
    }

    fun GameTestDriver.tapForMana(you: EntityId, land: EntityId, landName: String) {
        val abilityId = cardRegistry.requireCard(landName).activatedAbilities[0].id
        submit(ActivateAbility(playerId = you, sourceId = land, abilityId = abilityId)).error shouldBe null
    }

    fun GameTestDriver.chooseTargetIfAsked(you: EntityId, creature: EntityId) {
        if (pendingDecision is ChooseTargetsDecision) submitTargetSelection(you, listOf(creature)).error shouldBe null
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.junkCount(you: EntityId): Int = getPermanents(you).count { getCardName(it) == "Junk" }

    test("fortify attaches C.A.M.P. only to a land you control") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val forest = d.putLandOnBattlefield(you, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.giveColorlessMana(you, 3)
        val action = d.legalActions(you).single { (it.action as? ActivateAbility)?.abilityId == fortifyId }
        action.description shouldBe "Fortify {3}"
        (bear in action.validTargets!!) shouldBe false
        (forest in action.validTargets!!) shouldBe true
    }

    test("tapping the fortified land for green: counter on a green creature, plus a Junk token") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val forest = d.putLandOnBattlefield(you, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, forest)

        d.tapForMana(you, forest, "Forest")
        d.chooseTargetIfAsked(you, bear)
        d.stackSize shouldBe 1
        d.bothPass()

        d.plusOneCounters(bear) shouldBe 1
        d.junkCount(you) shouldBe 1
    }

    test("mana of a color the creature doesn't have: counter only") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val island = d.putLandOnBattlefield(you, "Island")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, island)

        d.tapForMana(you, island, "Island")
        d.chooseTargetIfAsked(you, bear)
        d.bothPass()

        d.plusOneCounters(bear) shouldBe 1
        d.junkCount(you) shouldBe 0
    }

    test("colorless mana shares no color: counter only") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val land = d.putLandOnBattlefield(you, "CAMP Test Wastes")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, land)

        d.tapForMana(you, land, "CAMP Test Wastes")
        d.chooseTargetIfAsked(you, bear)
        d.bothPass()

        d.plusOneCounters(bear) shouldBe 1
        d.junkCount(you) shouldBe 0
    }

    test("the auto-payer tapping the fortified land also triggers, with the color it produced") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val forest = d.putLandOnBattlefield(you, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, forest)

        val spell = d.putCardInHand(you, "CAMP Test Bear")
        d.submit(CastSpell(playerId = you, cardId = spell, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null
        d.chooseTargetIfAsked(you, bear)
        d.isTapped(forest) shouldBe true
        d.bothPass()

        d.plusOneCounters(bear) shouldBe 1
        d.junkCount(you) shouldBe 1
    }

    test("tapping an unfortified land does nothing") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val fortified = d.putLandOnBattlefield(you, "Forest")
        val other = d.putLandOnBattlefield(you, "Forest")
        d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, fortified)

        d.tapForMana(you, other, "Forest")
        d.stackSize shouldBe 0
    }

    test("an explicitly chosen source paying a cycling cost still reports the color it produced") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val forest = d.putLandOnBattlefield(you, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, forest)

        val cycler = d.putCardInHand(you, "CAMP Test Cycler")
        d.submit(
            CycleCard(playerId = you, cardId = cycler, paymentStrategy = PaymentStrategy.Explicit(listOf(forest)))
        ).error shouldBe null
        d.isTapped(forest) shouldBe true
        d.chooseTargetIfAsked(you, bear)
        d.bothPass()

        d.plusOneCounters(bear) shouldBe 1
        d.junkCount(you) shouldBe 1
    }

    test("a land adding two colors through two effects reports both: the second color still matches") {
        val d = driver()
        val you = d.activePlayer!!
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val land = d.putLandOnBattlefield(you, "CAMP Test Simic Land")
        val merfolk = d.putCreatureOnBattlefield(you, "CAMP Test Merfolk")
        d.fortify(you, camp, land)

        d.tapForMana(you, land, "CAMP Test Simic Land")
        d.chooseTargetIfAsked(you, merfolk)
        d.bothPass()

        d.plusOneCounters(merfolk) shouldBe 1
        d.junkCount(you) shouldBe 1
    }

    test("an opponent tapping the fortified land triggers C.A.M.P. for its controller") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val camp = d.putPermanentOnBattlefield(you, "C.A.M.P.")
        val forest = d.putLandOnBattlefield(you, "Forest")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.fortify(you, camp, forest)
        // The opponent gains control of the fortified land; C.A.M.P. stays attached (CR 301.6
        // only restricts what Fortify can target).
        d.replaceState(d.state.updateEntity(forest) { it.with(ControllerComponent(opponent)) })

        d.passPriority(you)
        d.priorityPlayer shouldBe opponent
        d.tapForMana(opponent, forest, "Forest")
        d.chooseTargetIfAsked(you, bear)
        d.stackSize shouldBe 1
        d.bothPass()

        d.plusOneCounters(bear) shouldBe 1
        d.junkCount(you) shouldBe 1
        d.junkCount(opponent) shouldBe 0
    }
})
