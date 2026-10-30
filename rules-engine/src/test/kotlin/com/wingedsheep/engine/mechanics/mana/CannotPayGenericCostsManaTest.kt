package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.ActivateAbility
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
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [ManaRestriction.CannotPayGenericCosts] — Jegantha, the Wellspring's "{T}: Add {W}{U}{B}{R}{G}.
 * This mana can't be spent to pay generic mana costs." The mana pays colored symbols (and the
 * colored half of a monocolored hybrid) for any spell or ability, but never `{1}`, `{X}` or the
 * generic half of `{2/W}` (CR 107.4b, 107.4e).
 */
class CannotPayGenericCostsManaTest : FunSpec({

    val barred = ManaRestriction.CannotPayGenericCosts
    val creatureSpell = SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), isCreature = true, colorCount = 1)
    val ability = SpellPaymentContext(isAbilityActivation = true)

    fun wubrg(pool: ManaPool = ManaPool()) =
        Color.entries.fold(pool) { acc, color -> acc.addRestricted(color, 1, barred) }

    context("the restriction") {
        test("admits every payment, contextless ones included — it gates symbols, not payments") {
            barred.isSatisfiedBy(creatureSpell) shouldBe true
            barred.isSatisfiedBy(ability) shouldBe true
            barred.isSatisfiedBy(SpellPaymentContext(isTurnFaceUpAction = true)) shouldBe true
            barred.isEligibleForPayment(null) shouldBe true
        }

        test("only it, alone or in a conjunction, refuses generic symbols") {
            barred.paysGeneric shouldBe false
            ManaRestriction.AllOf(listOf(ManaRestriction.CreatureSpellsOnly, barred)).paysGeneric shouldBe false
            ManaRestriction.CreatureSpellsOnly.paysGeneric shouldBe true
            ManaRestriction.AnySpend.paysGeneric shouldBe true
        }
    }

    context("floating mana") {
        test("pays colored symbols for a spell and for an ability") {
            wubrg().canPay(ManaCost.parse("{W}{U}{B}"), creatureSpell) shouldBe true
            wubrg().canPay(ManaCost.parse("{R}{G}"), ability) shouldBe true
            wubrg().pay(ManaCost.parse("{W}{U}"), creatureSpell).shouldNotBeNull()
                .restrictedMana.map { it.color } shouldContainExactlyInAnyOrder listOf(Color.BLACK, Color.RED, Color.GREEN)
        }

        test("never pays generic mana") {
            wubrg().canPay(ManaCost.parse("{1}"), creatureSpell) shouldBe false
            wubrg().canPay(ManaCost.parse("{1}{W}"), creatureSpell) shouldBe false
            wubrg().pay(ManaCost.parse("{1}{W}"), creatureSpell).shouldBeNull()
            val partial = wubrg().payPartial(ManaCost.parse("{2}{W}"), creatureSpell)
            partial.remainingCost shouldBe ManaCost.parse("{2}")
            partial.newPool.restrictedMana.size shouldBe 4
        }

        test("never pays {X}") {
            wubrg().xCoverage(3, emptySet(), creatureSpell) shouldBe 0
            wubrg().allocateFloating(ManaCost.parse("{X}{W}"), creatureSpell, xAmount = 1).shouldBeNull()
        }

        test("pays a monocolored hybrid's colored half, not its generic half") {
            wubrg().canPay(ManaCost.parse("{2/W}"), creatureSpell) shouldBe true
            wubrg(ManaPool()).spendRestricted(Color.WHITE, creatureSpell).shouldNotBeNull()
                .canPay(ManaCost.parse("{2/W}"), creatureSpell) shouldBe false
        }

        test("generic is paid by other mana while the barred mana takes the colored symbol") {
            // A barred {W} and an unrestricted {W}: the barred one must go to the {W} pip.
            val pool = ManaPool().addRestricted(Color.WHITE, 1, barred).add(Color.WHITE)
            pool.canPay(ManaCost.parse("{1}{W}"), creatureSpell) shouldBe true
            pool.pay(ManaCost.parse("{1}{W}"), creatureSpell).shouldNotBeNull().isEmpty() shouldBe true
            // Two restricted {W}s, one barred: the barred one pays the pip, the other pays {1}.
            val both = ManaPool().addRestricted(Color.WHITE, 1, ManaRestriction.CreatureSpellsOnly)
                .addRestricted(Color.WHITE, 1, barred)
            both.pay(ManaCost.parse("{1}{W}"), creatureSpell).shouldNotBeNull().isEmpty() shouldBe true
        }

        test("a two-color hybrid takes the barred unit of either color, leaving the other for generic") {
            val pool = ManaPool().add(Color.RED).addRestricted(Color.GREEN, 1, barred)
            pool.canPay(ManaCost.parse("{R/G}{1}"), creatureSpell) shouldBe true
            pool.pay(ManaCost.parse("{R/G}{1}"), creatureSpell).shouldNotBeNull().isEmpty() shouldBe true
        }

        test("the exact allocator also keeps it off generic pips") {
            wubrg().allocateFloating(ManaCost.parse("{W}{U}"), creatureSpell).shouldNotBeNull()
            wubrg().allocateFloating(ManaCost.parse("{1}{W}"), creatureSpell).shouldBeNull()
        }
    }

    context("auto-tap and casting") {
        val wellspring = card("Wellspring Tester") {
            typeLine = "Creature — Elemental Elk"
            manaCost = "{4}{R/G}"
            colorIdentity = "RG"
            power = 5
            toughness = 5
            oracleText = "{T}: Add {W}{U}{B}{R}{G}. This mana can't be spent to pay generic mana costs."
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.WHITE, restriction = barred) then
                    Effects.AddMana(Color.BLUE, restriction = barred) then
                    Effects.AddMana(Color.BLACK, restriction = barred) then
                    Effects.AddMana(Color.RED, restriction = barred) then
                    Effects.AddMana(Color.GREEN, restriction = barred)
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }
        val bear = CardDefinition.creature(
            name = "Generic Bear", manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(com.wingedsheep.sdk.core.Subtype("Bear")), power = 2, toughness = 2,
        )
        val guildmage = CardDefinition.creature(
            name = "Two-Pip Mage", manaCost = ManaCost.parse("{W}{U}"),
            subtypes = setOf(com.wingedsheep.sdk.core.Subtype("Wizard")), power = 2, toughness = 2,
        )
        val allCards = TestCards.all + listOf(wellspring, bear, guildmage)
        fun solver() = ManaSolver(CardRegistry().also { it.register(allCards) }, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        fun driver(): GameTestDriver = GameTestDriver().also {
            it.registerCards(allCards)
            it.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        fun GameTestDriver.wellspring(): com.wingedsheep.sdk.model.EntityId =
            putCreatureOnBattlefield(player1, "Wellspring Tester").also { removeSummoningSickness(it) }

        test("solver taps it for colored pips only") {
            val d = driver()
            d.wellspring()
            solver().solve(d.state, d.player1, ManaCost.parse("{W}{U}")).shouldNotBeNull().sources.size shouldBe 1
            solver().solve(d.state, d.player1, ManaCost.parse("{1}{G}")).shouldBeNull()
            solver().solve(d.state, d.player1, ManaCost.parse("{G}"), xValue = 1).shouldBeNull()
        }

        test("solver gives the colored pip to it so a land can pay the generic") {
            val d = driver()
            d.wellspring()
            d.putLandOnBattlefield(d.player1, "Forest")
            val solution = solver().solve(d.state, d.player1, ManaCost.parse("{1}{G}")).shouldNotBeNull()
            solution.sources.map { it.name } shouldContainExactlyInAnyOrder listOf("Wellspring Tester", "Forest")
            // The four unspent colors float back still barred from generic.
            solution.remainingBonusMana.all { it.restriction == barred } shouldBe true
        }

        test("auto-pay taps it for the colored pip and a land for the generic") {
            val d = driver()
            val source = d.wellspring()
            val forest = d.putLandOnBattlefield(d.player1, "Forest")
            d.castSpell(d.player1, d.putCardInHand(d.player1, "Generic Bear")).error shouldBe null
            d.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
            d.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
            // Its four other colors float, still barred from generic.
            val left = d.state.getEntity(d.player1)!!.get<ManaPoolComponent>()!!.restrictedMana
            left.size shouldBe 4
            left.all { it.restriction == barred } shouldBe true
        }

        test("a spell auto-paid from its floating mana leaves the generic to a land") {
            val d = driver()
            val source = d.wellspring()
            val forest = d.putLandOnBattlefield(d.player1, "Forest")
            d.submit(ActivateAbility(d.player1, source, wellspring.activatedAbilities.single().id)).error shouldBe null
            d.state.getEntity(d.player1)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 5

            d.castSpell(d.player1, d.putCardInHand(d.player1, "Generic Bear")).error shouldBe null
            d.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
            d.state.getEntity(d.player1)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 4
        }

        test("without other mana a spell with generic in its cost can't be cast, a fully colored one can") {
            val d = driver()
            val source = d.wellspring()
            d.submit(ActivateAbility(d.player1, source, wellspring.activatedAbilities.single().id)).error shouldBe null
            d.castSpell(d.player1, d.putCardInHand(d.player1, "Generic Bear")).error shouldNotBe null
            d.castSpell(d.player1, d.putCardInHand(d.player1, "Two-Pip Mage")).error shouldBe null
        }
    }
})
