package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.ThreeTreeScribe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Three Tree Scribe:
 * {1}{G} Creature — Frog Druid 2/3
 * Whenever this creature or another creature you control leaves the battlefield
 * without dying, put a +1/+1 counter on target creature you control.
 *
 * Regression: the trigger is ANY-bound, so it must also fire when the Scribe itself
 * leaves the battlefield — the main battlefield trigger loop skips it because the
 * Scribe is no longer on the battlefield by the time the zone-change event fires.
 */
class ThreeTreeScribeTest : FunSpec({

    // Inline bounce spell used to force a non-lethal leaves-the-battlefield event.
    val Unsummon = card("Test Unsummon") {
        manaCost = "{U}"
        typeLine = "Instant"

        spell {
            target("creature", TargetCreature())
            effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)
        }
    }

    val allCards = TestCards.all + listOf(ThreeTreeScribe, Unsummon)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("trigger fires when Three Tree Scribe itself leaves the battlefield without dying") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Island" to 20),
            startingLife = 20
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Scribe on the battlefield, plus a teammate to receive the counter.
        val scribe = driver.putCreatureOnBattlefield(activePlayer, "Three Tree Scribe")
        val teammate = driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")

        // Bounce the Scribe back to its owner's hand.
        val unsummon = driver.putCardInHand(activePlayer, "Test Unsummon")
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = unsummon,
                targets = listOf(ChosenTarget.Permanent(scribe))
            )
        )
        result.isSuccess shouldBe true

        // Resolve the bounce — this must fire Three Tree Scribe's leaves-without-dying
        // trigger with the Scribe itself as the leaving creature.
        driver.bothPass()

        // Select the teammate as the target of the Scribe's triggered ability.
        driver.submitTargetSelection(activePlayer, listOf(teammate))
        driver.bothPass()

        // The teammate should have a +1/+1 counter from the Scribe's own leave trigger.
        val counters = driver.state.getEntity(teammate)?.get<CountersComponent>()
        val plusCounters = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusCounters shouldBe 1

        // Sanity: Scribe is no longer on the battlefield.
        driver.findPermanent(activePlayer, "Three Tree Scribe") shouldBe null
    }
})
