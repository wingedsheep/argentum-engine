package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SilumgarsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Silumgar's Command (DTK #232) — {3}{U}{B} Instant.
 *
 * Choose two —
 * • Counter target noncreature spell.
 * • Return target permanent to its owner's hand.
 * • Target creature gets -3/-3 until end of turn.
 * • Destroy target planeswalker.
 *
 * Pins the counter mode answering a noncreature spell (with the -3/-3 mode killing a 3/3), the
 * bounce + planeswalker-destruction pair, and that the counter mode refuses a *creature* spell.
 */
class SilumgarsCommandScenarioTest : FunSpec({

    // Mode order follows the printed bullets.
    val counterNoncreature = 0
    val bounce = 1
    val shrink = 2
    val destroyPlaneswalker = 3

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SilumgarsCommand))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castCommand(
        caster: EntityId,
        modes: List<Int>,
        modeTargets: List<List<ChosenTarget>>,
    ): ExecutionResult {
        giveMana(caster, Color.BLUE, 1)
        giveMana(caster, Color.BLACK, 1)
        giveColorlessMana(caster, 3)
        val spell = putCardInHand(caster, "Silumgar's Command")
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

    test("counter a noncreature spell and give a creature -3/-3") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        // The opponent bolts my creature; I answer at instant speed.
        driver.giveMana(opp, Color.RED, 1)
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.passPriority(me)
        driver.submit(
            CastSpell(playerId = opp, cardId = bolt, targets = listOf(ChosenTarget.Permanent(mine)))
        ).error shouldBe null
        driver.passPriority(opp)

        val boltSpell = driver.getTopOfStack().shouldNotBeNull()
        driver.castCommand(
            me,
            modes = listOf(counterNoncreature, shrink),
            modeTargets = listOf(
                listOf(ChosenTarget.Spell(boltSpell)),
                listOf(ChosenTarget.Permanent(theirs)),
            ),
        ).error shouldBe null
        driver.bothPass() // resolve the Command; the countered Bolt never resolves

        driver.assertInGraveyard(opp, "Lightning Bolt")
        driver.getPermanents(me) shouldContain mine
        driver.getGraveyard(opp) shouldContain theirs
    }

    test("bounce a permanent to its owner's hand and destroy a planeswalker") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val creature = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val planeswalker = driver.putPermanentOnBattlefield(opp, "Sarkhan Unbroken").also {
            driver.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 4)))
        }

        driver.castCommand(
            me,
            modes = listOf(bounce, destroyPlaneswalker),
            modeTargets = listOf(
                listOf(ChosenTarget.Permanent(creature)),
                listOf(ChosenTarget.Permanent(planeswalker)),
            ),
        ).error shouldBe null
        driver.bothPass()

        driver.getHand(opp) shouldContain creature
        driver.getGraveyard(opp) shouldContain planeswalker
    }

    test("the counter mode cannot target a creature spell") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val bears = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, bears).error shouldBe null
        val bearsSpell = driver.getTopOfStack().shouldNotBeNull()

        driver.castCommand(
            me,
            modes = listOf(counterNoncreature, shrink),
            modeTargets = listOf(
                listOf(ChosenTarget.Spell(bearsSpell)),
                listOf(ChosenTarget.Permanent(theirs)),
            ),
        ).error.shouldNotBeNull().lowercase() shouldContain "target"
        driver.getTopOfStack() shouldBe bearsSpell
    }
})
