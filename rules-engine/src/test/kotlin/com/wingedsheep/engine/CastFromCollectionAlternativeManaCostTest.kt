package com.wingedsheep.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Targets
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Resolution-time casting for a fixed alternative mana cost — the `alternativeManaCost` of
 * [CastFromCollectionWithoutPayingCostEffect], "you may cast it by paying {1} rather than paying its
 * mana cost" (Jadzi, Oracle of Arcavios), here straight off the top of the library.
 *
 * Rules pinned here: the fixed cost replaces the mana cost as an alternative cost (CR 118.9), so
 * colored pips are gone and {X} is 0 (CR 107.3b); cost increases still apply on top of it (CR
 * 601.2f); a caster who can't pay doesn't cast (CR 601.2h) and the card stays on top of the library
 * with nothing granted. Card-level coverage lives in `JadziOracleOfArcaviosScenarioTest`.
 */
class CastFromCollectionAlternativeManaCostTest : FunSpec({

    val Caster = card("Alternative Mana Cast Test") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(1), revealed = true)
                run(Effects.CastFromCollectionByPaying(top, "{1}"))
            }
        }
    }
    val XBlast = card("Alternative Mana X Blast") {
        manaCost = "{X}{R}{R}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.DealDamage(DynamicAmounts.xValue(), player) then
                Effects.GainLife(1, EffectTarget.Controller)
        }
    }
    val Taxer = card("Alternative Mana Taxer") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.AnyCaster(GameObjectFilter.Noncreature),
                modification = CostModification.IncreaseGeneric(1),
            )
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Caster, XBlast, Taxer))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.greenInPool(): Int = state.getEntity(player1)?.get<ManaPoolComponent>()?.green ?: 0

    /** Casts the test instant, then floats [green] mana for the resolution-time cast and resolves. */
    fun GameTestDriver.castCaster(green: Int) {
        val caster = putCardInHand(player1, "Alternative Mana Cast Test")
        giveMana(player1, Color.BLUE, 1)
        castSpell(player1, caster).error shouldBe null
        giveMana(player1, Color.GREEN, green)
        bothPass()
    }

    fun GameTestDriver.onTopOfLibrary(cardId: EntityId) =
        state.getZone(ZoneKey(player1, Zone.LIBRARY)).firstOrNull() == cardId

    test("the card is cast from the library paying {1} instead of its colored mana cost") {
        val d = newDriver()
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt") // {R}

        d.castCaster(green = 1)

        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.getStackSpellNames() shouldBe listOf("Lightning Bolt")
        d.greenInPool() shouldBe 0
        // The substitute cost was owed by that one cast; it doesn't ride the card onward.
        d.state.getEntity(bolt)?.get<PlayWithFixedAlternativeManaCostComponent>() shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("a caster who can't pay {1} casts nothing; the card stays on top with nothing granted") {
        val d = newDriver()
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        d.castCaster(green = 0)
        d.submitTargetSelection(d.player1, listOf(d.player2))

        d.getStackSpellNames() shouldBe emptyList()
        d.onTopOfLibrary(bolt) shouldBe true
        d.state.getEntity(bolt)!!.get<PlayWithFixedAlternativeManaCostComponent>() shouldBe null
        d.state.mayPlayPermissions.none { bolt in it.cardIds } shouldBe true
    }

    test("an {X} spell cast this way has X = 0") {
        val d = newDriver()
        d.putCardOnTopOfLibrary(d.player1, "Alternative Mana X Blast")

        d.castCaster(green = 1)
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.getStackSpellNames() shouldBe listOf("Alternative Mana X Blast")
        d.bothPass()

        d.getLifeTotal(d.player2) shouldBe 20
        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("a cost increase still applies on top of the alternative cost") {
        val d = newDriver()
        d.putCreatureOnBattlefield(d.player2, "Alternative Mana Taxer")
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        // The Caster instant itself is taxed too: {U} + {1}.
        val caster = d.putCardInHand(d.player1, "Alternative Mana Cast Test")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, caster).error shouldBe null
        d.giveMana(d.player1, Color.GREEN, 1)
        d.bothPass()
        d.submitTargetSelection(d.player1, listOf(d.player2))
        // {1} + {1} tax: one green isn't enough.
        d.getStackSpellNames() shouldBe emptyList()
        d.onTopOfLibrary(bolt) shouldBe true
    }

    test("the cost increase is payable: {1} + {1} tax casts it") {
        val d = newDriver()
        d.putCreatureOnBattlefield(d.player2, "Alternative Mana Taxer")
        d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        val caster = d.putCardInHand(d.player1, "Alternative Mana Cast Test")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.castSpell(d.player1, caster).error shouldBe null
        d.giveMana(d.player1, Color.GREEN, 2)
        d.bothPass()
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.getStackSpellNames() shouldBe listOf("Lightning Bolt")
        d.greenInPool() shouldBe 0
    }

    test("a stamp whose one-shot permission is gone no longer prices a later cast") {
        // A cast cancelled mid-announcement (a modal prompt) leaves the stamp behind with its
        // permission revoked; the card, drawn later, must cost its printed mana cost again.
        val d = newDriver()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        val (stalePermission, withId) = d.state.newEntity()
        d.replaceState(withId.updateEntity(bolt) {
            it.with(PlayWithFixedAlternativeManaCostComponent(d.player1, ManaCost.parse("{1}"), grantedPermissionId = stalePermission))
        })

        d.giveMana(d.player1, Color.GREEN, 1)
        d.submitExpectFailure(CastSpell(d.player1, bolt, listOf(ChosenTarget.Player(d.player2))))
        d.giveMana(d.player1, Color.RED, 1)
        d.submit(CastSpell(d.player1, bolt, listOf(ChosenTarget.Player(d.player2)))).error shouldBe null
        d.getStackSpellNames() shouldBe listOf("Lightning Bolt")
    }

    test("the SDK shape: a fixed mana cost replaces the mana cost and reads as the Oracle text does") {
        val effect = Effects.CastFromCollectionByPaying(CollectionSlot("card"), "{1}")
            as CastFromCollectionWithoutPayingCostEffect
        effect.alternativeManaCost shouldBe ManaCost.parse("{1}")
        effect.payManaCost shouldBe false
        effect.description shouldBe "Cast that card by paying {1} rather than paying its mana cost"
        shouldThrow<IllegalArgumentException> {
            CastFromCollectionWithoutPayingCostEffect(
                from = "card", payManaCost = true, alternativeManaCost = ManaCost.parse("{1}")
            )
        }
    }
})
