package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.AdditionalCombatPhasesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.GreatTrainHeist
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue

/**
 * Great Train Heist — Spree instant ({R} + per-mode costs):
 * + {2}{R} — Untap all creatures you control. If it's your combat phase, there is an
 *            additional combat phase after this phase.
 * + {2} — Creatures you control get +1/+0 and gain first strike until end of turn.
 * + {R} — Choose target opponent. Whenever a creature you control deals combat damage to
 *         that player this turn, create a tapped Treasure token.
 *
 * Mode 3 exercises the new recipient-scoped delayed trigger (`watchedRecipient`): the
 * Treasure is created only on combat damage dealt to the *chosen* opponent by a creature the
 * caster controls.
 */
class GreatTrainHeistTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + GreatTrainHeist + PredefinedTokens.Treasure)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun treasureCount(driver: GameTestDriver, playerId: EntityId): Int =
        driver.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).count { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Treasure"
        }

    test("mode 3: a creature you control dealing combat damage to the chosen opponent makes a tapped Treasure") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val attacker = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        driver.removeSummoningSickness(attacker)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Mode 2 (index 2) only: base {R} + additional {R}.
        driver.giveMana(me, Color.RED, 2)
        val spell = driver.putCardInHand(me, "Great Train Heist")
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(2),
                targets = listOf(ChosenTarget.Player(opp)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve the spell -> registers the delayed trigger

        driver.state.delayedTriggers.size shouldBe 1

        // Swing with the Bears at the chosen opponent.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)
        driver.bothPass() // end declare attackers
        driver.bothPass() // end declare blockers (none)
        // Resolve combat damage + the resulting delayed-trigger treasure.
        repeat(8) { driver.bothPass() }

        treasureCount(driver, me) shouldBe 1
        // The Treasure entered tapped.
        val treasure = driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).first { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Treasure"
        }
        driver.isTapped(treasure).shouldBeTrue()
    }

    test("mode 2: creatures you control get +1/+0 and gain first strike until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Mode 1 (index 1): base {R} + additional {2}.
        driver.giveMana(me, Color.RED, 1)
        driver.giveColorlessMana(me, 2)
        val spell = driver.putCardInHand(me, "Great Train Heist")
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(1),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(bear) shouldBe 3
        projected.hasKeyword(bear, Keyword.FIRST_STRIKE).shouldBeTrue()
    }

    test("mode 1: untaps your creatures and grants an additional combat phase during your combat") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val attacker = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)

        // Attack so the creature is tapped and we are in the combat phase. After declaring
        // attackers the active player still holds priority, so we can cast in response.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opp)
        driver.isTapped(attacker).shouldBeTrue()

        // Cast Great Train Heist mode 0 (index 0) during combat: {2}{R} + base {R}.
        driver.giveMana(me, Color.RED, 2)
        driver.giveColorlessMana(me, 2)
        val spell = driver.putCardInHand(me, "Great Train Heist")
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(0),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Creatures you control untapped.
        driver.isTapped(attacker) shouldBe false
        // An additional combat phase is queued because it was cast during the caster's combat.
        driver.state.getEntity(me)?.get<AdditionalCombatPhasesComponent>() shouldBe
            AdditionalCombatPhasesComponent(1)
    }
})
