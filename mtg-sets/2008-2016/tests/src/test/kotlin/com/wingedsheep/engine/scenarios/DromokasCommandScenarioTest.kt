package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.DromokasCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dromoka's Command (DTK #221) — {G}{W} Instant.
 *
 * Choose two —
 * • Prevent all damage target instant or sorcery spell would deal this turn.
 * • Target player sacrifices an enchantment of their choice.
 * • Put a +1/+1 counter on target creature.
 * • Target creature you control fights target creature you don't control.
 *
 * The card's signature line is the third and fourth modes together on one creature: the same
 * object answers two separate instances of "target" (CR 115.3), and the modes resolve in printed
 * order, so the counter is already on the creature when it deals its fight damage. A 2/2 fighting a
 * 1/3 only kills it if the counter landed first.
 */
class DromokasCommandScenarioTest : FunSpec({

    val projector = StateProjector()

    // Mode order follows the printed bullets.
    val preventSpell = 0
    val counter = 2
    val fight = 3

    val Sentinel = CardDefinition.creature(
        name = "Test Sentinel",
        manaCost = ManaCost.parse("{2}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 3
    )

    val LightningBolt = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Lightning Bolt deals 3 damage to any target.",
        script = CardScript.spell(
            effect = DealDamageEffect(3, EffectTarget.ContextTarget(0)),
            AnyTarget()
        )
    )

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DromokasCommand, Sentinel, LightningBolt))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castCommand(
        caster: EntityId,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): ExecutionResult {
        giveMana(caster, Color.GREEN, 1)
        giveMana(caster, Color.WHITE, 1)
        val spell = putCardInHand(caster, "Dromoka's Command")
        return submit(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = modeTargets.flatten(),
                chosenModes = modes,
                modeTargetsOrdered = modeTargets,
            )
        )
    }

    fun GameTestDriver.plusOneCounters(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("the same creature takes the counter and then fights with it") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        val sentinel = driver.putCreatureOnBattlefield(opp, "Test Sentinel") // 1/3

        driver.castCommand(
            me,
            modes = listOf(counter, fight),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(bear)),
                listOf(ChosenTarget.Permanent(bear), ChosenTarget.Permanent(sentinel)),
            ),
        ).error shouldBe null
        driver.bothPass()

        // The counter resolved first, so the bear hit for 3 and the 1/3 is gone.
        driver.findPermanent(opp, "Test Sentinel") shouldBe null
        driver.findPermanent(me, "Grizzly Bears") shouldNotBe null
        driver.plusOneCounters(bear) shouldBe 1
        projector.getProjectedPower(driver.state, bear) shouldBe 3
    }

    test("the prevention mode stops the damage of a targeted instant") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2

        // The opponent bolts the bear; we respond with the Command.
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opp, bolt, listOf(bear)).outcome shouldBe Outcome.Done
        driver.passPriority(opp)
        val boltOnStack = driver.getTopOfStack()!!

        driver.castCommand(
            me,
            modes = listOf(preventSpell, counter),
            modeTargets = listOf(
                listOf(ChosenTarget.Spell(boltOnStack)),
                listOf(ChosenTarget.Permanent(bear)),
            ),
        ).error shouldBe null

        driver.bothPass() // Dromoka's Command
        driver.bothPass() // Lightning Bolt

        driver.findPermanent(me, "Grizzly Bears") shouldNotBe null
        driver.plusOneCounters(bear) shouldBe 1
    }
})
