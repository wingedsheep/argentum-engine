package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Add two mana of different colors" — `Effects.AddManaOfDifferentColors`
 * (`AddDynamicManaEffect.differentColors`), Firemind Vessel / Guild Globe / Interplanar Beacon.
 *
 * The two units must be two different colors, so one activation pays {U}{R} or {1}{G} but never
 * {G}{G}. That holds on every path: the manual activation's second color prompt leaves out the
 * first color, and the auto-payer models the second unit as "any color but the one already made".
 */
class ManaOfDifferentColorsTest : FunSpec({

    val vessel = card("Test Different Vessel") {
        manaCost = "{4}"
        typeLine = "Artifact"
        oracleText = "{T}: Add two mana of different colors."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddManaOfDifferentColors(2)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val beacon = card("Test Different Beacon") {
        manaCost = ""
        typeLine = "Land"
        oracleText = "{T}: Add {C}.\n{1}, {T}: Add two mana of different colors. Spend this mana only to cast planeswalker spells."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
            effect = Effects.AddManaOfDifferentColors(
                2, restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(CardType.PLANESWALKER)
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    fun bear(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val izzetBear = bear("Test Izzet Bear", "{U}{R}")
    val greenGreenBear = bear("Test Double Green Bear", "{G}{G}")
    val genericBear = bear("Test Generic Green Bear", "{1}{G}")

    val cards = TestCards.all + listOf(vessel, beacon, izzetBear, greenGreenBear, genericBear)

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun solver() = ManaSolver(CardRegistry().apply { register(cards) }, PredicateEvaluator(cardRegistry = null))

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("the source is a two-mana, all-color source flagged as different colors") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Test Different Vessel")

        val source = solver().findAvailableManaSources(d.state, you).single { it.name == "Test Different Vessel" }
        source.producesColors shouldBe Color.entries.toSet()
        source.manaAmount shouldBe 2
        source.differentColors shouldBe true
    }

    test("one activation pays a two-color cost, crediting both colors") {
        val d = driver()
        val you = d.activePlayer!!
        val vesselId = d.putPermanentOnBattlefield(you, "Test Different Vessel")

        val solution = solver().solve(d.state, you, ManaCost.parse("{U}{R}")).shouldNotBeNull()
        solution.sources.map { it.entityId } shouldBe listOf(vesselId)
        solution.manaProduced.getValue(vesselId).coloredAmounts shouldBe mapOf(Color.BLUE to 1, Color.RED to 1)
        solution.remainingBonusMana shouldBe emptyList()
    }

    test("one activation never pays two pips of the same color") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Test Different Vessel")

        solver().solve(d.state, you, ManaCost.parse("{G}{G}")).shouldBeNull()
        solver().solve(d.state, you, ManaCost.parse("{1}{G}")).shouldNotBeNull()
    }

    test("a lone pip floats the second unit as a different color") {
        val d = driver()
        val you = d.activePlayer!!
        val vesselId = d.putPermanentOnBattlefield(you, "Test Different Vessel")

        val solution = solver().solve(d.state, you, ManaCost.parse("{G}")).shouldNotBeNull()
        val produced = solution.manaProduced.getValue(vesselId)
        produced.total shouldBe 2
        produced.coloredAmounts.keys.size shouldBe 2
        produced.coloredAmounts shouldContainKey Color.GREEN
        solution.remainingBonusMana.single().color shouldNotBe Color.GREEN
    }

    test("auto-pay casts a two-color spell off a lone source") {
        val d = driver()
        val you = d.activePlayer!!
        val vesselId = d.putPermanentOnBattlefield(you, "Test Different Vessel")
        val spell = d.putCardInHand(you, "Test Izzet Bear")

        d.castSpell(you, spell).error shouldBe null
        d.bothPass()

        d.state.getBattlefield(you) shouldContain spell
        d.state.getEntity(vesselId)?.has<TappedComponent>() shouldBe true
    }

    test("auto-pay refuses a double-pip spell a lone source can't make") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Test Different Vessel")
        val spell = d.putCardInHand(you, "Test Double Green Bear")

        d.castSpell(you, spell).error.shouldNotBeNull()
        d.state.getBattlefield(you) shouldNotContain spell
    }

    test("auto-pay casts a one-pip spell, the second unit paying generic") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Test Different Vessel")
        val spell = d.putCardInHand(you, "Test Generic Green Bear")

        d.castSpell(you, spell).error shouldBe null
        d.bothPass()
        d.state.getBattlefield(you) shouldContain spell
    }

    test("a manual activation's second prompt leaves out the first color") {
        val d = driver()
        val you = d.activePlayer!!
        val vesselId = d.putPermanentOnBattlefield(you, "Test Different Vessel")

        d.submit(ActivateAbility(you, vesselId, vessel.activatedAbilities.single().id)).error shouldBe null
        val first = d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        first.availableColors shouldBe Color.entries.toSet()
        d.submitDecision(you, ColorChosenResponse(first.id, Color.GREEN)).error shouldBe null

        val second = d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        second.availableColors shouldBe Color.entries.toSet() - Color.GREEN
        d.submitDecision(you, ColorChosenResponse(second.id, Color.BLUE)).error shouldBe null

        d.pendingDecision.shouldBeNull()
        d.pool(you).green shouldBe 1
        d.pool(you).blue shouldBe 1
    }

    test("a manual activation can't name the first color twice") {
        val d = driver()
        val you = d.activePlayer!!
        val vesselId = d.putPermanentOnBattlefield(you, "Test Different Vessel")

        d.submit(ActivateAbility(you, vesselId, vessel.activatedAbilities.single().id)).error shouldBe null
        d.submitDecision(you, ColorChosenResponse(d.pendingDecision!!.id, Color.RED)).error shouldBe null
        d.submitDecision(you, ColorChosenResponse(d.pendingDecision!!.id, Color.RED)).error.shouldNotBeNull()
    }

    test("a restricted different-colors ability never pays for a spell outside its restriction") {
        val d = driver()
        val you = d.activePlayer!!
        d.putLandOnBattlefield(you, "Test Different Beacon")
        d.putLandOnBattlefield(you, "Plains")
        val spell = d.putCardInHand(you, "Test Izzet Bear")

        // Plains funds the {1}, but the two colors may only be spent on planeswalker spells.
        d.castSpell(you, spell).error.shouldNotBeNull()
        d.state.getBattlefield(you) shouldNotContain spell
    }
})
