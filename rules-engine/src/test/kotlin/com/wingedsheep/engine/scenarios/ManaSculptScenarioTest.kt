package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.ManaSculpt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mana Sculpt (SOS #57).
 *
 * "{1}{U}{U} Instant. Counter target spell. If you control a Wizard, add an amount of {C} equal to
 *  the amount of mana spent to cast that spell at the beginning of your next main phase."
 *
 * Exercises the delayed-trigger mana-spent snapshot: the countered spell is gone by the time the
 * delayed trigger fires, so [com.wingedsheep.engine.handlers.effects.composite.CreateDelayedTriggerExecutor]
 * captures "the amount of mana spent to cast that spell" into a fixed amount while the spell is still
 * on the stack. The Wizard rider only happens when the caster controls a Wizard.
 */
class ManaSculptScenarioTest : FunSpec({

    // A 1/1 Wizard for {U} so the controller "controls a Wizard".
    val Apprentice = CardDefinition.creature(
        name = "Apprentice Wizard",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype("Human"), Subtype("Wizard")),
        power = 1,
        toughness = 1
    )

    // A 3/3 for {2}{G} — total mana spent = 3 — to be the countered spell.
    val Bear = CardDefinition.creature(
        name = "Big Bear",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 3,
        toughness = 3
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ManaSculpt, Apprentice, Bear))
        return driver
    }

    fun colorlessInPool(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getEntity(playerId)?.get<ManaPoolComponent>()?.colorless ?: 0

    fun advanceToPlayersPrecombatMain(driver: GameTestDriver, playerId: EntityId, maxSteps: Int = 200) {
        var steps = 0
        while (!(driver.state.activePlayerId == playerId && driver.state.step == Step.PRECOMBAT_MAIN) &&
            steps++ < maxSteps
        ) {
            if (driver.state.pendingDecision != null) {
                driver.autoResolveDecision()
            } else if (driver.state.priorityPlayerId != null) {
                driver.passPriority(driver.state.priorityPlayerId!!)
            }
        }
        check(driver.state.activePlayerId == playerId && driver.state.step == Step.PRECOMBAT_MAIN) {
            "Failed to reach player's precombat main (active=${driver.state.activePlayerId}, step=${driver.state.step})"
        }
    }

    test("controlling a Wizard adds {C} equal to the mana spent, at the next main phase") {
        val driver = createDriver()
        // Player 2 starts so player 1 (the counterer) gets the next precombat main.
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20, startingPlayer = 1)

        val player2 = driver.activePlayer!!
        val player1 = driver.getOpponent(player2)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 1 controls a Wizard.
        driver.putCreatureOnBattlefield(player1, "Apprentice Wizard")

        // Player 2 casts Big Bear, paying {2}{G} from pool so 3 mana is recorded as spent.
        val bear = driver.putCardInHand(player2, "Big Bear")
        driver.giveMana(player2, Color.GREEN, 1)
        driver.giveColorlessMana(player2, 2)
        driver.castSpell(player2, bear)
        driver.getTopOfStackName() shouldBe "Big Bear"

        // Player 2 passes priority; player 1 responds with Mana Sculpt, countering Big Bear.
        driver.passPriority(player2)
        val sculpt = driver.putCardInHand(player1, "Mana Sculpt")
        driver.giveMana(player1, Color.BLUE, 2)
        driver.giveColorlessMana(player1, 1)
        val cast = driver.castSpellWithTargets(player1, sculpt, listOf(ChosenTarget.Spell(bear)))
        cast.isSuccess shouldBe true

        // Resolve the stack: Mana Sculpt counters Big Bear.
        driver.bothPass()
        driver.bothPass()
        driver.stackSize shouldBe 0

        // No mana added yet — it waits for the next main phase.
        colorlessInPool(driver, player1) shouldBe 0

        // Advance to player 1's next precombat main; the delayed trigger fires and resolves.
        advanceToPlayersPrecombatMain(driver, player1)
        driver.bothPass()

        // {C}{C}{C} added (3 mana was spent to cast Big Bear).
        colorlessInPool(driver, player1) shouldBe 3
    }

    test("no Wizard means no mana rider — the spell is still countered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20, startingPlayer = 1)

        val player2 = driver.activePlayer!!
        val player1 = driver.getOpponent(player2)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Player 2 casts Big Bear.
        val bear = driver.putCardInHand(player2, "Big Bear")
        driver.giveMana(player2, Color.GREEN, 1)
        driver.giveColorlessMana(player2, 2)
        driver.castSpell(player2, bear)

        // Player 2 passes priority; player 1 counters with Mana Sculpt but controls no Wizard.
        driver.passPriority(player2)
        val sculpt = driver.putCardInHand(player1, "Mana Sculpt")
        driver.giveMana(player1, Color.BLUE, 2)
        driver.giveColorlessMana(player1, 1)
        driver.castSpellWithTargets(player1, sculpt, listOf(ChosenTarget.Spell(bear)))

        driver.bothPass()
        driver.bothPass()
        driver.stackSize shouldBe 0

        advanceToPlayersPrecombatMain(driver, player1)
        driver.bothPass()

        // No Wizard → no delayed trigger → no mana.
        colorlessInPool(driver, player1) shouldBe 0
    }
})
