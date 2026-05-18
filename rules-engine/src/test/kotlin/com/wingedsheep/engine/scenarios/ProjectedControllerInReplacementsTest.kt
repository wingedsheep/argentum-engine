package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ktk.cards.HardenedScales
import com.wingedsheep.mtg.sets.definitions.ons.cards.BlatantThievery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression tests for the projection-vs-base-state bug in replacement-effect filters
 * that check `RecipientFilter.CreatureYouControl` / `PermanentYouControl`.
 *
 * The previous implementation read the recipient's controller from base
 * `ControllerComponent`, which misses control-changing continuous effects (e.g.,
 * Blatant Thievery, Annex). As a result, Hardened Scales would not boost counters
 * placed on a creature you stole, and damage prevention/amplification keyed to
 * "creature you control" would ignore the stolen creature.
 *
 * Each test steals an opponent's creature via Blatant Thievery and verifies that
 * the relevant replacement effect now treats the stolen creature as "you control"
 * under projection.
 */
class ProjectedControllerInReplacementsTest : FunSpec({

    val projector = StateProjector()

    // Inline test card with default DoubleDamage shape — defaults to
    // recipient = RecipientFilter.CreatureYouControl, source = Any.
    val TestDoubleDamageAura = card("Test Double Damage Source") {
        manaCost = "{3}{R}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to a creature you control, it deals double that damage instead."
        replacementEffect(
            DoubleDamage(
                appliesTo = GameEvent.DamageEvent(recipient = RecipientFilter.CreatureYouControl)
            )
        )
    }

    // Inline card: prevent 2 damage to any creature you control.
    val TestPreventTwoAura = card("Test Prevent Two") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to a creature you control, prevent 2 of that damage."
        replacementEffect(
            PreventDamage(
                amount = 2,
                appliesTo = GameEvent.DamageEvent(recipient = RecipientFilter.CreatureYouControl)
            )
        )
    }

    // Inline card: +1 damage to any creature you control.
    val TestExtraDamageAura = card("Test Extra Damage") {
        manaCost = "{2}{R}"
        typeLine = "Enchantment"
        oracleText = "If a source would deal damage to a creature you control, it deals 1 more damage."
        replacementEffect(
            ModifyDamageAmount(
                modifier = 1,
                appliesTo = GameEvent.DamageEvent(recipient = RecipientFilter.CreatureYouControl)
            )
        )
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                HardenedScales,
                BlatantThievery,
                TestDoubleDamageAura,
                TestPreventTwoAura,
                TestExtraDamageAura,
            )
        )
        return driver
    }

    test("Hardened Scales adds +1 counter when placing on a creature stolen via Blatant Thievery") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Active player controls Hardened Scales.
        driver.putPermanentOnBattlefield(activePlayer, "Hardened Scales")

        // Opponent controls a Centaur Courser. Base ControllerComponent.playerId = opponent.
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(theirCreature)

        // Active player steals it via Blatant Thievery.
        val thievery = driver.putCardInHand(activePlayer, "Blatant Thievery")
        driver.giveMana(activePlayer, Color.BLUE, 7)
        driver.castSpell(activePlayer, thievery, listOf(theirCreature))
        driver.bothPass()

        withClue("Projection reflects the steal — active player is now the controller") {
            projector.project(driver.state).getController(theirCreature) shouldBe activePlayer
        }

        // Place 1 +1/+1 counter on the stolen creature. Hardened Scales' filter is
        // RecipientFilter.CreatureYouControl. Under the previous bug, the filter compared
        // the *base* controller (still the opponent) and rejected the stolen creature, so
        // no modifier applied (result: 1). Fixed code uses projection (active player),
        // and the modifier applies (result: 2).
        val modified = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state = driver.state,
            targetId = theirCreature,
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            placerId = activePlayer
        )
        modified shouldBe 2
    }

    test("DoubleDamage (CreatureYouControl) doubles damage dealt to a stolen creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Test Double Damage Source")

        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(theirCreature)

        val thievery = driver.putCardInHand(activePlayer, "Blatant Thievery")
        driver.giveMana(activePlayer, Color.BLUE, 7)
        driver.castSpell(activePlayer, thievery, listOf(theirCreature))
        driver.bothPass()

        // 3 damage to the stolen creature should be doubled to 6 under projection.
        val amplified = DamageUtils.applyStaticDamageAmplification(
            state = driver.state,
            targetId = theirCreature,
            amount = 3,
            sourceId = null
        )
        amplified shouldBe 6
    }

    test("ModifyDamageAmount (CreatureYouControl) adds modifier to damage dealt to a stolen creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Test Extra Damage")

        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(theirCreature)

        val thievery = driver.putCardInHand(activePlayer, "Blatant Thievery")
        driver.giveMana(activePlayer, Color.BLUE, 7)
        driver.castSpell(activePlayer, thievery, listOf(theirCreature))
        driver.bothPass()

        // 3 damage to stolen creature gets +1 → 4 under projection.
        val amplified = DamageUtils.applyStaticDamageAmplification(
            state = driver.state,
            targetId = theirCreature,
            amount = 3,
            sourceId = null
        )
        amplified shouldBe 4
    }

    test("PreventDamage (CreatureYouControl) reduces damage dealt to a stolen creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Test Prevent Two")

        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.removeSummoningSickness(theirCreature)

        val thievery = driver.putCardInHand(activePlayer, "Blatant Thievery")
        driver.giveMana(activePlayer, Color.BLUE, 7)
        driver.castSpell(activePlayer, thievery, listOf(theirCreature))
        driver.bothPass()

        // Deal 3 damage to the stolen creature through the public DamageUtils entry point.
        // This routes through the private applyStaticDamageReduction (which we cannot call
        // directly), so this test covers the DamageUtils.applyStaticDamageReduction fix and
        // — by code equivalence with DamageCalculator.estimateDamagePrevention — that
        // sibling fix as well.
        val result = DamageUtils.dealDamageToTarget(
            state = driver.state,
            targetId = theirCreature,
            amount = 3,
            sourceId = null
        )

        val damageDealt = result.state.getEntity(theirCreature)?.get<DamageComponent>()?.amount ?: 0
        // 3 incoming − 2 prevented = 1 marked.
        damageDealt shouldBe 1
    }
})
