package com.wingedsheep.engine.mana

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.PendingOneSpellAnyManaType
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `SpendAnyManaTypeForSpells(filter, asThoughAnyColor = true)` — "you may spend mana as though it
 * were mana of any color to cast [these] spells" (Oath of Nissa).
 *
 * Rules pinned here: spending mana as though it were mana of any color lets any mana pay a colored
 * pip, but colorless is a mana *type*, not a color, so a `{C}` pip still needs colorless mana —
 * unlike "mana of any type can be spent" (CR 118.14), which relaxes `{C}` too. The amount owed is
 * unchanged; the grant is zone-agnostic (a spell cast from the top of the library is relaxed like
 * one cast from hand); and when an any-type grant also matches, the stronger one applies.
 */
class SpendManaAsThoughAnyColorForSpellsTest : FunSpec({

    val AnyColorGranter = card("Any Color Granter") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        staticAbility { ability = SpendAnyManaTypeForSpells(GameObjectFilter.Sorcery, asThoughAnyColor = true) }
    }

    val AnyTypeGranter = card("Any Type Granter") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        staticAbility { ability = SpendAnyManaTypeForSpells(GameObjectFilter.Sorcery) }
    }

    val TopCaster = card("Top Sorcery Caster") {
        manaCost = "{G}"
        typeLine = "Enchantment"
        staticAbility { ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.Sorcery) }
    }

    val BlueSorcery = card("Blue Pip Sorcery") {
        manaCost = "{1}{U}{U}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    val ColorlessPip = card("Colorless Pip Sorcery") {
        manaCost = "{1}{C}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    val PhyrexianSorcery = card("Phyrexian Pip Sorcery") {
        manaCost = "{1}{U/P}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(AnyColorGranter, AnyTypeGranter, TopCaster, BlueSorcery, ColorlessPip, PhyrexianSorcery)
        )
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Sorcery-speed casts: act as whoever's main phase it is.
    fun GameTestDriver.me() = activePlayer!!

    fun GameTestDriver.redInPool(): Int = state.getEntity(me())?.get<ManaPoolComponent>()?.red ?: 0

    fun GameTestDriver.offered(name: String, zone: String? = null): Boolean =
        legalActions(me()).any {
            it.actionType == "CastSpell" && it.description.contains(name) && (zone == null || it.sourceZone == zone)
        }

    test("red mana pays blue pips, and exactly the printed amount is spent") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        val sorcery = d.putCardInHand(d.me(), "Blue Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 4)

        d.offered("Blue Pip Sorcery") shouldBe true
        d.castSpell(d.me(), sorcery).error shouldBe null
        d.redInPool() shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.me()) shouldBe 23
    }

    test("a {C} pip is not relaxed: colored mana alone can't pay it") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        val sorcery = d.putCardInHand(d.me(), "Colorless Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 2)

        d.offered("Colorless Pip Sorcery") shouldBe false
        (d.castSpell(d.me(), sorcery).error != null) shouldBe true
    }

    test("a {C} pip is paid by colorless mana alongside the relaxed colored pips") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        val sorcery = d.putCardInHand(d.me(), "Colorless Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 1)
        d.giveColorlessMana(d.me(), 1)

        d.offered("Colorless Pip Sorcery") shouldBe true
        d.castSpell(d.me(), sorcery).error shouldBe null
    }

    test("an any-type grant matching the same spell wins: red mana pays the {C} pip") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        d.putPermanentOnBattlefield(d.me(), "Any Type Granter")
        val sorcery = d.putCardInHand(d.me(), "Colorless Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 2)

        d.offered("Colorless Pip Sorcery") shouldBe true
        d.castSpell(d.me(), sorcery).error shouldBe null
        d.redInPool() shouldBe 0
    }

    test("zone-agnostic: a blue sorcery cast from the top of the library is relaxed too") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        d.putPermanentOnBattlefield(d.me(), "Top Sorcery Caster")
        val sorcery = d.putCardOnTopOfLibrary(d.me(), "Blue Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 3)

        d.offered("Blue Pip Sorcery", zone = "LIBRARY") shouldBe true
        d.castSpell(d.me(), sorcery).error shouldBe null
        d.redInPool() shouldBe 0
    }

    test("a Phyrexian pip stays payable with 2 life: only mana is spent as though any color") {
        val d = newDriver()
        d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        val sorcery = d.putCardInHand(d.me(), "Phyrexian Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 1)

        d.submit(
            CastSpell(
                d.me(), sorcery,
                paymentStrategy = PaymentStrategy.Explicit(emptyList(), phyrexianLifePayments = listOf(Color.BLUE))
            )
        ).error shouldBe null
        d.getLifeTotal(d.me()) shouldBe 18
        d.redInPool() shouldBe 0
    }

    test("a waiting any-type rider still pays the {C} pip the any-color static leaves") {
        val d = newDriver()
        val granter = d.putPermanentOnBattlefield(d.me(), "Any Color Granter")
        d.replaceState(
            d.state.copy(
                pendingAnyManaTypeSpells = listOf(
                    PendingOneSpellAnyManaType(d.me(), GameObjectFilter.Any, granter, "Test Rider")
                )
            )
        )
        val sorcery = d.putCardInHand(d.me(), "Colorless Pip Sorcery")
        d.giveMana(d.me(), Color.RED, 2)

        d.offered("Colorless Pip Sorcery") shouldBe true
        d.castSpell(d.me(), sorcery).error shouldBe null
        d.state.pendingAnyManaTypeSpells shouldBe emptyList()
    }
})
