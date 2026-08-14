package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.TheDeathOfGwenStacy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Death of Gwen Stacy — {2}{B} Enchantment — Saga (SPM #54).
 *
 *   I   — Destroy target creature.
 *   II  — Each player may discard a card. Each player who doesn't loses 3 life.
 *   III — Exile any number of target players' graveyards.
 *
 * Generic saga machinery (lore accrual, chapter triggers, sacrifice after the final chapter) is
 * covered by CreatureSagaTest; this pins the three chapter effects. Later chapters are reached by
 * advancing turns (the natural lore accrual) via a callback-driven pump that answers the chapter
 * decisions the way each scenario needs.
 */
class TheDeathOfGwenStacyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheDeathOfGwenStacy))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castGwen(caster: EntityId): EntityId {
        val spell = putCardInHand(caster, "The Death of Gwen Stacy")
        giveMana(caster, Color.BLACK, 1)
        giveColorlessMana(caster, 2)
        castSpell(caster, spell)
        return spell
    }

    /**
     * Drive the game forward answering decisions, stopping once [predicate] holds. Chapter target
     * choices route through [targets]; every "may discard" (and any other yes/no) through [yesNo];
     * anything else auto-resolves (yes/no auto-resolve declines, which is a natural "who doesn't").
     */
    fun GameTestDriver.pumpUntil(
        yesNo: (YesNoDecision) -> Boolean = { false },
        targets: (ChooseTargetsDecision) -> Map<Int, List<EntityId>> = { emptyMap() },
        maxSteps: Int = 5000,
        predicate: GameTestDriver.() -> Boolean
    ) {
        var guard = 0
        while (!predicate() && guard++ < maxSteps) {
            val pd = state.pendingDecision
            when {
                pd is ChooseTargetsDecision -> submitMultiTargetSelection(pd.playerId, targets(pd))
                pd is YesNoDecision -> submitYesNo(pd.playerId, yesNo(pd))
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

    fun GameTestDriver.inExile(player: EntityId, id: EntityId): Boolean =
        state.zones[ZoneKey(player, Zone.EXILE)]?.contains(id) == true

    test("chapter I — destroys target creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val lions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        driver.castGwen(me)

        // The Saga resolves, chapter I triggers and targets the only creature — destroy it.
        driver.pumpUntil(targets = { mapOf(0 to listOf(lions)) }) {
            findPermanent(opp, "Savannah Lions") == null
        }

        driver.findPermanent(opp, "Savannah Lions") shouldBe null
        driver.getGraveyard(opp).contains(lions) shouldBe true
    }

    test("chapter II — each player who doesn't discard loses 3 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val startMe = driver.getLifeTotal(me)
        val startOpp = driver.getLifeTotal(opp)

        // No creatures on board, so chapter I fizzles with no legal target.
        driver.castGwen(me)

        // Advance until chapter II (lore 2) resolves. Every "may discard" is declined, so each
        // player takes the 3-life hit.
        driver.pumpUntil(yesNo = { false }) {
            getLifeTotal(me) <= startMe - 3 && getLifeTotal(opp) <= startOpp - 3
        }

        driver.getLifeTotal(me) shouldBe startMe - 3
        driver.getLifeTotal(opp) shouldBe startOpp - 3
    }

    test("chapter II — a player who discards loses no life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val startMe = driver.getLifeTotal(me)
        val startOpp = driver.getLifeTotal(opp)

        driver.castGwen(me)

        // The Saga's controller discards (says yes); the opponent declines and loses 3 life.
        driver.pumpUntil(yesNo = { pd -> pd.playerId == me }) {
            getLifeTotal(opp) <= startOpp - 3
        }

        // Discarding satisfies the chapter, so the controller loses no life.
        driver.getLifeTotal(me) shouldBe startMe
        driver.getLifeTotal(opp) shouldBe startOpp - 3
    }

    test("chapter III — exiles the targeted players' graveyards") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val seededMe = driver.putCardInGraveyard(me, "Savannah Lions")
        val seededOpp = driver.putCardInGraveyard(opp, "Savannah Lions")

        driver.castGwen(me)

        // Advance to chapter III (lore 3), targeting both players' graveyards for exile.
        driver.pumpUntil(
            yesNo = { false },
            targets = { pd -> mapOf(0 to (pd.legalTargets[0] ?: emptyList())) }
        ) {
            !getGraveyard(me).contains(seededMe) && !getGraveyard(opp).contains(seededOpp)
        }

        // Both seeded cards left their graveyards and are now in exile.
        driver.getGraveyard(me).contains(seededMe) shouldBe false
        driver.getGraveyard(opp).contains(seededOpp) shouldBe false
        driver.inExile(me, seededMe) shouldBe true
        driver.inExile(opp, seededOpp) shouldBe true
    }
})
