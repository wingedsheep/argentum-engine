package com.wingedsheep.engine

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Resolution-time casting where "mana of any type can be spent to cast that spell" — the
 * `withAnyManaType` of [CastFromCollectionWithoutPayingCostEffect] (Summon: Esper Valigarmanda's
 * chapters II–IV).
 *
 * Rules pinned here: mana of any type may be spent as though it were colorless mana or mana of any
 * color (CR 118.14) — so red mana pays a blue pip *and* a `{C}` pip — while the cost itself is
 * unchanged (CR 609.4b: the mana value and the amount owed stay what they were); the cast is made
 * during resolution, ignoring the card's timing (CR 608.2g); a cost that still can't be paid means
 * nothing is cast (CR 601.2h); and the rider covers that one cast only — without it the same red
 * mana can't pay, and nothing of the grant outlives the cast. Card-level coverage lives in
 * `SummonEsperValigarmandaScenarioTest`.
 */
class CastFromCollectionAnyManaTypeTest : FunSpec({

    // "Exile cards from the top of your library until you exile a nonland card. Cast that card,
    // and mana of any type can be spent to cast that spell." No "may": the payment is under test.
    val AnyTypeCaster = card("Any Type Cast Test") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val (nonland, exiled) = gatherUntilMatch(GameObjectFilter.Nonland)
                exile(exiled)
                run(Effects.CastFromCollection(nonland, withAnyManaType = true))
            }
        }
    }

    // The same shape without the rider — the control.
    val PlainCaster = card("Plain Cast Test") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val (nonland, exiled) = gatherUntilMatch(GameObjectFilter.Nonland)
                exile(exiled)
                run(Effects.CastFromCollection(nonland))
            }
        }
    }

    // A colorless pip: only colorless mana pays {C} unless mana of any type can be spent.
    val ColorlessPip = card("Colorless Pip Sorcery") {
        manaCost = "{1}{C}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AnyTypeCaster, PlainCaster, ColorlessPip))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.redInPool(): Int = state.getEntity(player1)?.get<ManaPoolComponent>()?.red ?: 0

    /** Casts [casterName] with [red] extra red floating for the resolution-time cast. */
    fun GameTestDriver.castCaster(casterName: String, red: Int) {
        val caster = putCardInHand(player1, casterName)
        giveMana(player1, Color.RED, 1)
        castSpell(player1, caster).error shouldBe null
        giveMana(player1, Color.RED, red)
        bothPass()
    }

    fun GameTestDriver.isExiled(cardId: EntityId) = cardId in state.getZone(ZoneKey(player1, Zone.EXILE))

    test("red mana pays a blue cost: the creature is cast on resolution, timing ignored") {
        val d = newDriver()
        val warrior = d.putCardOnTopOfLibrary(d.player1, "Phantom Warrior") // {1}{U}{U}

        d.castCaster("Any Type Cast Test", red = 3)

        d.getStackSpellNames() shouldBe listOf("Phantom Warrior")
        d.redInPool() shouldBe 0
        // The one-shot grant was spent by the cast; nothing of it rides the card onward.
        d.state.mayPlayPermissions.none { warrior in it.cardIds } shouldBe true
        d.bothPass()
        d.findPermanent(d.player1, "Phantom Warrior") shouldBe warrior
    }

    test("mana of any type covers a colorless pip too, and the amount owed is unchanged") {
        val d = newDriver()
        d.putCardOnTopOfLibrary(d.player1, "Colorless Pip Sorcery") // {1}{C}

        d.castCaster("Any Type Cast Test", red = 3)

        d.getStackSpellNames() shouldBe listOf("Colorless Pip Sorcery")
        withClue("{1}{C} still costs two mana") { d.redInPool() shouldBe 1 }
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("without the rider the same red mana can't pay: nothing is cast, nothing is spent or left granted") {
        val d = newDriver()
        val warrior = d.putCardOnTopOfLibrary(d.player1, "Phantom Warrior")

        d.castCaster("Plain Cast Test", red = 3)

        d.getStackSpellNames() shouldBe emptyList()
        d.redInPool() shouldBe 3
        d.isExiled(warrior) shouldBe true
        d.state.mayPlayPermissions.none { warrior in it.cardIds } shouldBe true
    }

    test("too little mana of any type still casts nothing, and the rider doesn't linger") {
        val d = newDriver()
        val warrior = d.putCardOnTopOfLibrary(d.player1, "Phantom Warrior")

        d.castCaster("Any Type Cast Test", red = 2)

        d.getStackSpellNames() shouldBe emptyList()
        d.redInPool() shouldBe 2
        d.isExiled(warrior) shouldBe true
        d.state.mayPlayPermissions.none { warrior in it.cardIds } shouldBe true
    }

    test("the SDK shape: the rider needs a paid cast and reads as the Oracle text does") {
        val effect = Effects.CastFromCollection(CollectionSlot("card"), withAnyManaType = true)
            as CastFromCollectionWithoutPayingCostEffect
        effect.withAnyManaType shouldBe true
        effect.payManaCost shouldBe true
        effect.description shouldBe "Cast that card, and mana of any type can be spent to cast that spell"
        shouldThrow<IllegalArgumentException> {
            CastFromCollectionWithoutPayingCostEffect(from = "card", withAnyManaType = true)
        }
    }
})
