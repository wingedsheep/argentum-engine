package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.KravensLastHunt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kraven's Last Hunt — {3}{G} Enchantment — Saga (SPM #105).
 *
 *   I   — Mill five cards. When you do, this Saga deals damage equal to the greatest power among
 *         creature cards in your graveyard to target creature.
 *   II  — Target creature you control gets +2/+2 until end of turn.
 *   III — Return target creature card from your graveyard to your hand.
 *
 * Generic saga machinery (lore accrual, chapter triggers, sacrifice after the final chapter) is
 * covered by CreatureSagaTest; this pins the three chapter effects. Later chapters are reached by
 * passing priority through successive turns (the natural lore accrual), answering each chapter's
 * target choice by picking the sole legal target.
 */
class KravensLastHuntScenarioTest : FunSpec({

    // Vanilla creatures with known power/toughness so the graveyard-max-power math is deterministic.
    val bigBeast = card("Kraven Test Beast") {
        typeLine = "Creature — Beast"
        power = 5
        toughness = 5
    }
    val wall = card("Kraven Test Wall") {
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
    }
    val bear = card("Kraven Test Bear") {
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KravensLastHunt, bigBeast, wall, bear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castKraven(caster: EntityId): EntityId {
        val spell = putCardInHand(caster, "Kraven's Last Hunt")
        giveMana(caster, Color.GREEN, 1)
        giveColorlessMana(caster, 3)
        castSpell(caster, spell)
        return spell
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0
    fun GameTestDriver.markedDamage(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    /**
     * Drive the game forward answering decisions until [predicate] holds. Every chapter target
     * choice picks the first legal target of the first requirement; yes/no auto-declines.
     */
    fun GameTestDriver.pumpUntil(
        maxSteps: Int = 5000,
        predicate: GameTestDriver.() -> Boolean
    ) {
        var guard = 0
        while (!predicate() && guard++ < maxSteps) {
            val pd = state.pendingDecision
            when {
                pd is ChooseTargetsDecision ->
                    submitMultiTargetSelection(pd.playerId, mapOf(0 to (pd.legalTargets[0]?.take(1) ?: emptyList())))
                pd is YesNoDecision -> submitYesNo(pd.playerId, false)
                pd != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
                else -> break
            }
        }
        if (!predicate()) {
            error("pumpUntil: predicate not met after $guard steps (step=${state.step})")
        }
    }

    test("chapter I — mills five and deals damage equal to greatest graveyard creature power") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Seed a 5-power creature in my graveyard; the only battlefield creature is the opponent's
        // 0/8 wall, so chapter I must target it and deal exactly 5 (its 8 toughness survives it).
        driver.putCardInGraveyard(me, "Kraven Test Beast")
        val theWall = driver.putCreatureOnBattlefield(opp, "Kraven Test Wall")
        val gyBefore = driver.getGraveyard(me).size

        driver.castKraven(me)

        driver.pumpUntil { markedDamage(theWall) >= 5 }

        // Damage equals the greatest power among creature cards in the graveyard (the 5/5 Beast).
        driver.markedDamage(theWall) shouldBe 5
        // The five milled cards (basic Forests) landed in the graveyard.
        driver.getGraveyard(me).size shouldBe gyBefore + 5
    }

    test("chapter II — target creature you control gets +2/+2 until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // The only creature is my 2/2 Bear. Chapter I (on entry) finds no creature cards in my
        // graveyard, so it deals 0 to the Bear; chapter II then pumps it to 4/4.
        val theBear = driver.putCreatureOnBattlefield(me, "Kraven Test Bear")

        driver.castKraven(me)

        driver.pumpUntil { power(theBear) >= 4 }

        driver.power(theBear) shouldBe 4
        driver.toughness(theBear) shouldBe 4
    }

    test("chapter III — returns target creature card from your graveyard to your hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // No creatures on the battlefield, so chapters I and II have no legal target and are skipped;
        // chapter III returns the seeded creature card from the graveyard to hand.
        val bearCard = driver.putCardInGraveyard(me, "Kraven Test Bear")

        driver.castKraven(me)

        driver.pumpUntil { getHand(me).contains(bearCard) }

        driver.getHand(me).contains(bearCard) shouldBe true
        driver.getGraveyard(me).contains(bearCard) shouldBe false
    }
})
