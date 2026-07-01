package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.FireNationCadets
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fire Nation Cadets (TLA) — {R} Creature — Human Soldier, 1/2.
 *
 * - "This creature has firebending 2 as long as there's a Lesson card in your graveyard.
 *    (Whenever this creature attacks, add {R}{R}. This mana lasts until end of combat.)"
 * - "{2}: This creature gets +1/+0 until end of turn."
 *
 * The conditional firebending is the heart of the card: a [Scope.Self] GrantTriggeredAbility
 * installing [firebendingAttackTrigger(2)] gated by a graveyard condition. Firebending has no
 * engine handler — the printed keyword is a display tag + an attack-triggered combat-duration
 * mana effect — so these tests assert the *behavior* (attacking produces red combat-duration mana)
 * rather than a projected keyword. The toggle is pinned both ways: with no Lesson in the graveyard
 * the grant is off and attacking adds nothing; with a Lesson present it adds {R}{R}.
 */
class FireNationCadetsScenarioTest : FunSpec({

    // A minimal Lesson card to seed the graveyard condition.
    val lesson = card("Test Lesson") {
        manaCost = "{R}"
        typeLine = "Instant — Lesson"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FireNationCadets, lesson))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    /** Fully resolve the stack, resolving every triggered ability that lands on it. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.combatMana(playerId: EntityId) =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .filter { it.expiry == ManaExpiry.END_OF_COMBAT }

    test("with no Lesson in your graveyard, attacking adds no firebending mana") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val cadets = driver.putCreatureOnBattlefield(me, "Fire Nation Cadets")
        driver.removeSummoningSickness(cadets)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(cadets), opponent)
        driver.resolveStack()

        // The conditional firebending grant is off — no combat-duration mana.
        driver.combatMana(me).size shouldBe 0
    }

    test("with a Lesson in your graveyard, it has firebending 2 — attacking adds {R}{R}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putCardInGraveyard(me, "Test Lesson")
        val cadets = driver.putCreatureOnBattlefield(me, "Fire Nation Cadets")
        driver.removeSummoningSickness(cadets)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(cadets), opponent)
        driver.resolveStack()

        val combat = driver.combatMana(me)
        combat.size shouldBe 2
        combat.all { it.color == Color.RED } shouldBe true
    }

    test("{2}: this creature gets +1/+0 until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cadets = driver.putCreatureOnBattlefield(me, "Fire Nation Cadets")
        driver.state.projectedState.getPower(cadets) shouldBe 1

        // Pay {2} and activate the pump.
        driver.giveMana(me, Color.RED, 2)
        val pump = FireNationCadets.activatedAbilities[0].id
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = cadets, abilityId = pump))
        driver.resolveStack()

        driver.state.projectedState.getPower(cadets) shouldBe 2
        driver.state.projectedState.getToughness(cadets) shouldBe 2
    }
})
