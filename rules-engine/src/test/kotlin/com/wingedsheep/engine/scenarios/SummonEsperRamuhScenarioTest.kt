package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.SummonEsperRamuh
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Summon: Esper Ramuh (FIN) — {2}{R}{R} Enchantment Creature — Saga Wizard 3/3.
 *
 *  I — Judgment Bolt — This creature deals damage equal to the number of noncreature, nonland
 *      cards in your graveyard to target creature an opponent controls.
 *  II, III — Wizards you control get +1/+0 until end of turn.
 *
 * Generic saga machinery (lore accrual, chapter triggers, final-chapter sacrifice) is covered by
 * [CreatureSagaTest]; this pins this card's two distinct chapter behaviours.
 */
class SummonEsperRamuhScenarioTest : FunSpec({

    val projector = StateProjector()

    // A noncreature, nonland card to seed the graveyard (counts toward Judgment Bolt).
    val fillerInstant = card("Ramuh Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Ramuh Test Bolt deals 1 damage to any target."
    }
    // A creature card in the graveyard — must NOT count toward Judgment Bolt.
    val fillerCreature = card("Ramuh Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    // A land card in the graveyard — must NOT count toward Judgment Bolt.
    val fillerLand = card("Ramuh Test Waste") {
        typeLine = "Land"
    }
    // A fat opponent creature that survives the bolt, so we can read the exact marked damage.
    val opponentWall = card("Ramuh Test Wall") {
        manaCost = "{4}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 5
    }
    // A vanilla Wizard the controller owns, to observe the chapters II/III team pump.
    val alliedWizard = card("Ramuh Test Mage") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Wizard"
        power = 2
        toughness = 2
    }

    fun newDriver(extra: List<com.wingedsheep.sdk.model.CardDefinition>): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SummonEsperRamuh) + extra)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
    }

    // Resolve everything currently on the stack without ever passing priority while a decision is
    // pending (no targeted chapter is in play in the pump test, so any decision is auto-resolved).
    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 60) {
            when {
                driver.pendingDecision != null -> driver.autoResolveDecision()
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }
    }

    test("Chapter I deals damage equal to noncreature, nonland cards in your graveyard to a target opponent creature") {
        val driver = newDriver(listOf(fillerInstant, fillerCreature, fillerLand, opponentWall))
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Three noncreature, nonland cards count; the creature card and the land card do not.
        repeat(3) { driver.putCardInGraveyard(me, "Ramuh Test Bolt") }
        driver.putCardInGraveyard(me, "Ramuh Test Bear")
        driver.putCardInGraveyard(me, "Ramuh Test Waste")

        val wall = driver.putCreatureOnBattlefield(opponent, "Ramuh Test Wall")

        val saga = driver.putCardInHand(me, "Summon: Esper Ramuh")
        driver.giveMana(me, Color.RED, 4)
        driver.castSpell(me, saga)

        // Resolve the saga spell, then its chapter I trigger — targeting the opposing Wall.
        var guard = 0
        while (guard++ < 60) {
            when (val pending = driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(pending.playerId, listOf(wall))
                null -> if (driver.state.stack.isNotEmpty()) driver.bothPass() else break
                else -> driver.autoResolveDecision()
            }
        }

        val damage = driver.state.getEntity(wall)?.get<DamageComponent>()?.amount ?: 0
        withClue("Judgment Bolt counts only the 3 noncreature, nonland cards (not the creature or land)") {
            damage shouldBe 3
        }
        // The 0/5 Wall took 3 < 5, so it is still on the battlefield.
        driver.findPermanent(opponent, "Ramuh Test Wall") shouldBe wall
    }

    test("Chapters II/III pump: Wizards you control get +1/+0 until end of turn") {
        val driver = newDriver(listOf(alliedWizard))
        val me = driver.activePlayer!!

        val wizard = driver.putCreatureOnBattlefield(me, "Ramuh Test Mage")
        projector.project(driver.state).getPower(wizard) shouldBe 2

        // Cast the saga. With no opposing creature in play, chapter I has no legal target and is
        // removed, so it simply enters as a lore-1 Saga creature.
        val saga = driver.putCardInHand(me, "Summon: Esper Ramuh")
        driver.giveMana(me, Color.RED, 4)
        driver.castSpell(me, saga)
        settle(driver)

        // Advance to the controller's next precombat main: lore 2 → chapter II fires.
        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // controller's next turn
        driver.state.activePlayerId shouldBe me
        settle(driver)

        // Chapter II resolved this turn: Wizards you control get +1/+0 until end of turn.
        val projected = projector.project(driver.state)
        withClue("Wizard got +1/+0 from chapter II") {
            projected.getPower(wizard) shouldBe 3
            projected.getToughness(wizard) shouldBe 2
        }
        // The saga itself is a Wizard, so it is pumped too (3/3 base -> 4/3).
        val sagaPerm = driver.findPermanent(me, "Summon: Esper Ramuh")!!
        projected.getPower(sagaPerm) shouldBe 4
        projected.getToughness(sagaPerm) shouldBe 3
    }
})
