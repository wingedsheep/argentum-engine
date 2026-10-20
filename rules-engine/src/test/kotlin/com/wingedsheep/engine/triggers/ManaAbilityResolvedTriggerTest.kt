package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine coverage for "whenever a mana ability of this creature resolves" —
 * `EventPattern.ManaAbilityResolved`, spelled `Triggers.self.manaAbilityResolves()`, with the amount
 * of mana produced read through `DynamicAmounts.triggerManaProduced()` (Tyvar the Bellicose).
 *
 * The trigger must fire however the mana ability resolved — a manual activation, a pause for a
 * color choice, or the auto-payer tapping the creature — for any mana ability, {T} in its cost or
 * not, and only for this permanent's own. It isn't a mana ability (it doesn't add mana), so it uses
 * the stack (CR 605.1b). The amount is what the ability itself produced; mana a separate triggered
 * mana ability adds on top is that ability's.
 */
class ManaAbilityResolvedTriggerTest : FunSpec({

    fun com.wingedsheep.sdk.dsl.CardBuilder.growsByManaProduced(oncePerTurn: Boolean = false) {
        triggeredAbility {
            trigger = Triggers.self.manaAbilityResolves()
            effect = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.triggerManaProduced(), EffectTarget.Self
            )
            this.oncePerTurn = oncePerTurn
        }
    }

    // {T}: Add {G}{G}.
    val twinDruid = card("Resolver Twin Druid") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Druid"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN, 2)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        growsByManaProduced()
    }

    // {T}: Add {G}; Pay 1 life: Add {G} — the second has no {T}. Triggers only once each turn.
    val bloodDruid = card("Resolver Blood Druid") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Druid"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        activatedAbility {
            cost = Costs.PayLife(1)
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        growsByManaProduced(oncePerTurn = true)
    }

    // {T}: Add one mana of any color — pauses for the color when none is pre-chosen.
    val prismDruid = card("Resolver Prism Druid") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Druid"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        growsByManaProduced()
    }

    // {T}: Add two mana in any combination of {R} and/or {G} — pauses for the split.
    val splitDruid = card("Resolver Split Druid") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Druid"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddManaInAnyCombination(2, setOf(Color.RED, Color.GREEN))
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        growsByManaProduced()
    }

    // A Mana Flare for creatures: a separate triggered mana ability adding {G}.
    val flare = card("Resolver Creature Flare") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility {
            ability = AdditionalManaOnSourceTap(sourceFilter = GameObjectFilter.Creature, color = Color.GREEN)
        }
    }

    val grove = card("Resolver Grove") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(Color.GREEN)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    val bear = card("Resolver Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(twinDruid, bloodDruid, prismDruid, splitDruid, flare, grove, bear))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("a manual activation fires the trigger on the stack with the amount produced") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Twin Druid")

        d.submit(ActivateAbility(you, druid, twinDruid.activatedAbilities.single().id)).error shouldBe null

        d.stackSize shouldBe 1
        d.plusOnes(druid) shouldBe 0
        d.bothPass()
        d.plusOnes(druid) shouldBe 2
    }

    test("a mana ability without {T} in its cost fires it too") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Blood Druid")

        d.submit(ActivateAbility(you, druid, bloodDruid.activatedAbilities[1].id)).error shouldBe null

        d.getLifeTotal(you) shouldBe 19
        d.stackSize shouldBe 1
        d.bothPass()
        d.plusOnes(druid) shouldBe 1
    }

    test("triggers only once each turn, however many mana abilities resolve") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Blood Druid")

        d.submit(ActivateAbility(you, druid, bloodDruid.activatedAbilities[1].id)).error shouldBe null
        d.bothPass()
        d.submit(ActivateAbility(you, druid, bloodDruid.activatedAbilities[0].id)).error shouldBe null
        d.submit(ActivateAbility(you, druid, bloodDruid.activatedAbilities[1].id)).error shouldBe null

        d.stackSize shouldBe 0
        d.plusOnes(druid) shouldBe 1
    }

    test("the auto-payer tapping the creature for a spell fires it") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Blood Druid")

        val spell = d.putCardInHand(you, "Resolver Bear")
        d.submit(CastSpell(you, spell, paymentStrategy = PaymentStrategy.Explicit(listOf(druid)))).error shouldBe null

        d.isTapped(druid) shouldBe true
        // The trigger goes on the stack above the spell, so it resolves first.
        d.stackSize shouldBe 2
        d.bothPass()
        d.plusOnes(druid) shouldBe 1
    }

    test("the auto-payer credits the whole production of a multi-mana creature") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Twin Druid")

        val spell = d.putCardInHand(you, "Resolver Bear")
        d.submit(CastSpell(you, spell, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        d.isTapped(druid) shouldBe true
        d.stackSize shouldBe 2
        d.bothPass()
        d.plusOnes(druid) shouldBe 2
    }

    test("a mana ability that pauses for its color fires it once the color is chosen") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Prism Druid")

        d.submit(ActivateAbility(you, druid, prismDruid.activatedAbilities.single().id)).error shouldBe null
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.RED)).error shouldBe null

        d.stackSize shouldBe 1
        d.bothPass()
        d.plusOnes(druid) shouldBe 1
    }

    test("an \"any combination of colors\" ability fires it with the whole amount once the split is chosen") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Split Druid")

        d.submit(ActivateAbility(you, druid, splitDruid.activatedAbilities.single().id)).error shouldBe null
        val decision = d.pendingDecision!!
        d.submitDecision(you, NumberChosenResponse(decision.id, 1)).error shouldBe null

        d.stackSize shouldBe 1
        d.bothPass()
        d.plusOnes(druid) shouldBe 2
    }

    test("mana a separate triggered mana ability adds isn't counted") {
        val d = driver()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Resolver Creature Flare")
        val druid = d.putPermanentOnBattlefield(you, "Resolver Twin Druid")

        d.submit(ActivateAbility(you, druid, twinDruid.activatedAbilities.single().id)).error shouldBe null
        d.bothPass()

        d.plusOnes(druid) shouldBe 2
    }

    test("another permanent's mana ability doesn't fire it") {
        val d = driver()
        val you = d.activePlayer!!
        val druid = d.putPermanentOnBattlefield(you, "Resolver Twin Druid")
        val land = d.putPermanentOnBattlefield(you, "Resolver Grove")

        d.submit(ActivateAbility(you, land, grove.activatedAbilities.single().id)).error shouldBe null

        d.stackSize shouldBe 0
        d.plusOnes(druid) shouldBe 0
    }
})
