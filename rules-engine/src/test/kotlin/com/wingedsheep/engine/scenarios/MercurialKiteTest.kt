package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Mercurial Kite.
 *
 * Mercurial Kite: {3}{U}
 * Creature — Bird
 * 2/1
 * Flying
 * Whenever Mercurial Kite deals combat damage to a creature,
 * tap that creature. It doesn't untap during its controller's next untap step.
 */
class MercurialKiteTest : FunSpec({

    val MercurialKite = CardDefinition.creature(
        name = "Mercurial Kite",
        manaCost = ManaCost.parse("{3}{U}"),
        subtypes = setOf(Subtype("Bird")),
        power = 2,
        toughness = 1,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhenever Mercurial Kite deals combat damage to a creature, tap that creature. It doesn't untap during its controller's next untap step.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = GameEvent.DealsDamageEvent(damageType = DamageType.Combat, recipient = RecipientFilter.AnyCreature),
                binding = TriggerBinding.SELF,
                effect = CompositeEffect(listOf(
                    TapUntapEffect(EffectTarget.TriggeringEntity, tap = true),
                    GrantKeywordUntilEndOfTurnEffect(Keyword.DOESNT_UNTAP, EffectTarget.TriggeringEntity, Duration.UntilYourNextTurn)
                ))
            )
        )
    )

    // A 1/4 with reach so it can block the flying Kite, both survive combat
    val ReachCreature = CardDefinition.creature(
        name = "Reach Creature",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Spider")),
        power = 1,
        toughness = 4,
        keywords = setOf(Keyword.REACH)
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MercurialKite, ReachCreature))
        return driver
    }

    test("taps creature that it deals combat damage to and prevents untap") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Mercurial Kite on the battlefield for player 1
        val kite = driver.putCreatureOnBattlefield(player1, "Mercurial Kite")
        driver.removeSummoningSickness(kite)

        // Put a 1/4 reach creature for player 2 (can block flyer, both survive)
        val target = driver.putCreatureOnBattlefield(player2, "Reach Creature")
        driver.removeSummoningSickness(target)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Player 1 attacks with Kite
        driver.declareAttackers(player1, listOf(kite), player2)
        driver.bothPass()

        // Player 2 blocks with the reach creature
        driver.declareBlockers(player2, mapOf(target to listOf(kite)))
        driver.bothPass()

        // Skip first strike damage
        driver.currentStep shouldBe Step.FIRST_STRIKE_COMBAT_DAMAGE
        driver.bothPass()

        // Combat damage - Kite deals 2 to Reach Creature, trigger fires
        driver.currentStep shouldBe Step.COMBAT_DAMAGE

        // Resolve the trigger (tap + doesn't untap)
        driver.bothPass()

        // Reach Creature should be tapped by the trigger
        driver.isTapped(target) shouldBe true

        // Advance to player 2's untap step (next turn)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.activePlayer shouldBe player2

        // Reach Creature should still be tapped (DOESNT_UNTAP prevented untapping)
        driver.isTapped(target) shouldBe true
    }

    test("creature untaps normally on subsequent untap step") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kite = driver.putCreatureOnBattlefield(player1, "Mercurial Kite")
        driver.removeSummoningSickness(kite)

        val target = driver.putCreatureOnBattlefield(player2, "Reach Creature")
        driver.removeSummoningSickness(target)

        // Attack and block
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player1, listOf(kite), player2)
        driver.bothPass()
        driver.declareBlockers(player2, mapOf(target to listOf(kite)))
        driver.bothPass()

        // Skip first strike, deal combat damage
        driver.bothPass()
        driver.bothPass()

        driver.isTapped(target) shouldBe true

        // Advance to player 2's upkeep (creature stays tapped during their untap)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.activePlayer shouldBe player2
        driver.isTapped(target) shouldBe true

        // Advance past P2's upkeep, through P2's turn, to P1's upkeep
        // (effect expires after P1's untap step)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.activePlayer shouldBe player1

        // Advance past P1's upkeep, through P1's turn, to P2's upkeep
        // Creature should untap this time (effect expired after P1's untap)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.activePlayer shouldBe player2

        // Reach Creature should now be untapped (effect expired)
        driver.isTapped(target) shouldBe false
    }

    test("trigger does not fire when dealing combat damage to a player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20
        )

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kite = driver.putCreatureOnBattlefield(attacker, "Mercurial Kite")
        driver.removeSummoningSickness(kite)

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Attack with kite (no blockers)
        driver.declareAttackers(attacker, listOf(kite), defender)
        driver.bothPass()

        // No blockers
        driver.declareNoBlockers(defender)
        driver.bothPass()

        // Skip first strike damage
        driver.bothPass()

        // Combat damage to player - trigger should NOT fire
        driver.assertLifeTotal(defender, 18)
    }
})
