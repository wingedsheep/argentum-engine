package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ContextPropertyKey.TARGETS_TOTAL_MANA_VALUE] — "the total mana value of the permanents this spell
 * targets" (CR 202.3), the summing sibling of `TARGET_COUNT`.
 *
 * It is read from two places that must agree, because Urgent Necropsy depends on them agreeing: a
 * *cost* is priced from the announced targets before it is paid (CR 601.2f), while an *effect* is
 * evaluated from the resolving object's target list. `CostAtomAmounts` and `DynamicAmountEvaluator`
 * are the two readers, and both sum the same list the same way. The card's own behaviour is pinned
 * by `UrgentNecropsyScenarioTest`; what is pinned here is the vocabulary underneath it, on the
 * resolution side and at its edges.
 */
class TargetsTotalManaValueTest : FunSpec({

    // "Draw cards equal to the total mana value of the permanents this spell targets."
    val probe = card("Evidence Probe") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            target = Targets.Creature
            effect = Effects.DrawCards(
                DynamicAmount.ContextProperty(ContextPropertyKey.TARGETS_TOTAL_MANA_VALUE)
            )
        }
    }

    // Same, but targeting a player — who has no mana value at all.
    val playerProbe = card("Bystander Probe") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            target = Targets.Player
            effect = Effects.DrawCards(
                DynamicAmount.ContextProperty(ContextPropertyKey.TARGETS_TOTAL_MANA_VALUE)
            )
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(probe, playerProbe))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an effect reads the summed mana value of the spell's targets") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Air Elemental is {3}{U}{U} — mana value 5.
        val elemental = driver.putCreatureOnBattlefield(opp, "Air Elemental")
        val spell = driver.putCardInHand(me, "Evidence Probe")
        driver.giveColorlessMana(me, 1)
        val handBefore = driver.getHandSize(me)

        val cast = driver.submit(
            CastSpell(me, spell, targets = listOf(ChosenTarget.Permanent(elemental)))
        )
        withClue("cast should succeed: ${cast.error}") { cast.isSuccess shouldBe true }
        driver.bothPass()

        // -1 for the spell leaving hand, +5 drawn.
        driver.getHandSize(me) shouldBe handBefore - 1 + 5
    }

    test("a targeted player contributes nothing — players have no mana value") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val spell = driver.putCardInHand(me, "Bystander Probe")
        driver.giveColorlessMana(me, 1)
        val handBefore = driver.getHandSize(me)

        val cast = driver.submit(
            CastSpell(me, spell, targets = listOf(ChosenTarget.Player(opp)))
        )
        withClue("cast should succeed: ${cast.error}") { cast.isSuccess shouldBe true }
        driver.bothPass()

        driver.getHandSize(me) shouldBe handBefore - 1
    }
})
