package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSpendingObligationsContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScopedManaAffordabilityTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
    fun scope(d: GameTestDriver, pool: ManaPoolComponent = ManaPoolComponent(), pending: Set<String> = emptySet()) {
        val p = d.activePlayer!!
        d.replaceState(d.state.updateEntity(p) { it.with(pool) }.pushContinuation(
            ManaSpendingObligationsContinuation(p, EffectContext(sourceId = null, controllerId = p), "scope", pending)))
    }
    fun canPay(d: GameTestDriver, cost: String) = d.services.manaSolver.canPay(
        d.state, d.activePlayer!!, ManaCost.parse(cost), spellContext = context)

    test("zero-output activation stays unaffordable despite sufficient untagged mana and lands") {
        val d = driver(); d.putLandOnBattlefield(d.activePlayer!!, "Forest")
        scope(d, ManaPoolComponent(green = 1), setOf("dry"))
        canPay(d, "{G}") shouldBe false
        canPay(d, "{0}") shouldBe false
    }
    test("unspent red activation cannot hide behind green pool payment") {
        val d = driver()
        scope(d, ManaPoolComponent(green = 1, restrictedMana = listOf(
            RestrictedManaEntry(Color.RED, ManaRestriction.AnySpend, obligationIds = setOf("red")))), setOf("red"))
        canPay(d, "{G}") shouldBe false
        canPay(d, "{G}{1}") shouldBe true
    }
    test("independent forest can complete a partial pool payment and no state is mutated") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest")
        scope(d, ManaPoolComponent(blue = 1))
        val before = d.state
        canPay(d, "{G}{U}") shouldBe true
        canPay(d, "{G}{G}{U}") shouldBe false
        d.state shouldBe before
    }
    test("each planned activation contributes while multi-mana excess is permitted") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Independent Double Green") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 2); manaAbility = true }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name)
        scope(d, ManaPoolComponent(restrictedMana = listOf(
            RestrictedManaEntry(Color.BLUE, ManaRestriction.AnySpend, obligationIds = setOf("blue")))), setOf("blue"))
        canPay(d, "{G}{1}") shouldBe true
        canPay(d, "{G}") shouldBe false
    }
    test("all containing scopes must be discharged during affordability") {
        val d = driver(); val p = d.activePlayer!!
        scope(d, ManaPoolComponent(green = 1), setOf("outer"))
        d.replaceState(d.state.pushContinuation(ManaSpendingObligationsContinuation(p,
            EffectContext(sourceId = null, controllerId = p), "inner")))
        canPay(d, "{G}") shouldBe false
    }
    test("bonus-only taps do not use aggregate fallback") {
        val d = driver(); val p = d.activePlayer!!
        val source = card("Independent Red Land") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.RED, 1); manaAbility = true }
        }
        val bonus = card("Independent Blue Bonus") {
            typeLine = "Enchantment"
            staticAbility { ability = com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap(
                sourceFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land, color = Color.BLUE) }
        }
        d.registerCards(listOf(source, bonus)); d.putLandOnBattlefield(p, source.name); d.putPermanentOnBattlefield(p, bonus.name)
        scope(d)
        canPay(d, "{U}") shouldBe false
        canPay(d, "{R}") shouldBe true
    }
    test("context-free affordability can prove ordinary independent mana") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest"); scope(d)
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}")) shouldBe true
    }
    test("excluded sources never enter a scoped affordability proof") {
        val d = driver(); val p = d.activePlayer!!
        val land = d.putLandOnBattlefield(p, "Forest"); scope(d)
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}"),
            excludeSources = setOf(land), spellContext = context) shouldBe false
    }
    test("projected source filters constrain independent planning") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest"); scope(d)
        d.replaceState(d.state.pushContinuation(com.wingedsheep.engine.core.ManaAbilitySourcesContinuation(
            p, com.wingedsheep.sdk.scripting.GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
            EffectContext(sourceId = null, controllerId = p))))
        canPay(d, "{G}") shouldBe false
    }

    test("intermediate mana activation may leave another identity for the final payment") {
        val d = driver(); val p = d.activePlayer!!
        scope(d, ManaPoolComponent(restrictedMana = listOf(
            RestrictedManaEntry(Color.GREEN, ManaRestriction.AnySpend, obligationIds = setOf("green")),
            RestrictedManaEntry(Color.BLUE, ManaRestriction.AnySpend, obligationIds = setOf("blue")))), setOf("green", "blue"))
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}"),
            spellContext = SpellPaymentContext(isAbilityActivation = true)) shouldBe true
        canPay(d, "{G}") shouldBe false
    }

    test("execution-backed affordability proves compound production") {
        val d = driver(); val p = d.activePlayer!!
        val source = card("Independent Compound Mana") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 1) then Effects.AddMana(Color.BLUE, 1)
                manaAbility = true
            }
        }
        d.registerCards(listOf(source)); d.putLandOnBattlefield(p, source.name)
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}"), spellContext = context) shouldBe true
        scope(d)
        canPay(d, "{G}") shouldBe true
    }

    test("tap-dependent continuous effects require execution-backed planning") {
        val d = driver(); val p = d.activePlayer!!
        val source = card("Dependent Mana Land") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
            staticAbility { ability = com.wingedsheep.sdk.scripting.ConditionalStaticAbility(
                com.wingedsheep.sdk.scripting.LoseAllAbilities(com.wingedsheep.sdk.scripting.filters.unified.GroupFilter(
                    com.wingedsheep.sdk.scripting.GameObjectFilter.Land)), Conditions.SourceIsTapped) }
        }
        d.registerCards(listOf(source)); repeat(2) { d.putLandOnBattlefield(p, source.name) }
        scope(d)
        canPay(d, "{G}{G}") shouldBe false
        d.giveMana(p, Color.GREEN, 2)
        canPay(d, "{G}{G}") shouldBe true
    }
    test("execution checks an untapped source filter before paying its tap cost") {
        val d = driver(); val p = d.activePlayer!!
        d.putLandOnBattlefield(p, "Forest"); scope(d)
        d.replaceState(d.state.pushContinuation(com.wingedsheep.engine.core.ManaAbilitySourcesContinuation(
            p, com.wingedsheep.sdk.scripting.GameObjectFilter.Land.untapped(),
            EffectContext(sourceId = null, controllerId = p))))
        canPay(d, "{G}") shouldBe true
        d.giveMana(p, Color.GREEN, 1)
        canPay(d, "{G}") shouldBe true
    }
    test("face-down boards have a uniform source-proof boundary and allow pool payments") {
        val hiddenCards = listOf(
            card("Hidden Mana Dependency") {
                typeLine = "Creature"; power = 2; toughness = 2
                staticAbility { ability = com.wingedsheep.sdk.scripting.DampLandManaProduction }
            },
            card("Hidden Vanilla Permanent") { typeLine = "Creature"; power = 2; toughness = 2 },
        )
        val producer = card("Hidden Preview Fixed Land") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 2); manaAbility = true }
        }
        for (hidden in hiddenCards) {
            val d = driver(); val p = d.activePlayer!!
            d.registerCards(listOf(hidden, producer)); val permanent = d.putPermanentOnBattlefield(p, hidden.name)
            d.replaceState(d.state.updateEntity(permanent) {
                it.with(com.wingedsheep.engine.state.components.identity.FaceDownComponent)
            })
            d.putLandOnBattlefield(p, producer.name); scope(d)
            canPay(d, "{G}{G}") shouldBe false
            d.giveMana(p, Color.GREEN, 2)
            canPay(d, "{G}{G}") shouldBe true
        }
    }
    test("hidden printed mana abilities never become hypothetical sources") {
        val d = driver(); val p = d.activePlayer!!
        val hidden = card("Hidden Printed Mana") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
        }
        d.registerCards(listOf(hidden)); val permanent = d.putLandOnBattlefield(p, hidden.name)
        d.replaceState(d.state.updateEntity(permanent) {
            it.with(com.wingedsheep.engine.state.components.identity.FaceDownComponent)
        })
        scope(d)
        canPay(d, "{G}") shouldBe false
    }

    test("context-free mixed sources cannot erase a colorless production restriction") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Mixed Restricted Colorless") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddColorlessMana(1, restriction = ManaRestriction.CreatureSpellsOnly)
                manaAbility = true
            }
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN); manaAbility = true }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name); scope(d)
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{C}")) shouldBe false
        canPay(d, "{C}") shouldBe false
        canPay(d, "{G}") shouldBe true
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{C}"),
            spellContext = SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), isCreature = true)) shouldBe true
    }

    test("context-free mixed sources cannot use a restricted ability's larger amount") {
        val d = driver(); val p = d.activePlayer!!
        val producer = card("Mixed Restricted Amount") {
            typeLine = "Land"
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN); manaAbility = true }
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddMana(Color.GREEN, 2, restriction = ManaRestriction.CreatureSpellsOnly)
                manaAbility = true
            }
        }
        d.registerCards(listOf(producer)); d.putLandOnBattlefield(p, producer.name); scope(d)
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}{G}")) shouldBe false
        canPay(d, "{G}{G}") shouldBe false
        canPay(d, "{G}") shouldBe true
        d.services.manaSolver.canPay(d.state, p, ManaCost.parse("{G}{G}"),
            spellContext = SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), isCreature = true)) shouldBe true
    }

})
