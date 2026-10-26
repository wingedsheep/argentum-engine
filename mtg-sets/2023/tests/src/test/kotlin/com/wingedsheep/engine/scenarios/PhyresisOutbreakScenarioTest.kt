package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onc.cards.PhyresisOutbreak
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Phyresis Outbreak (ONC #12) — {2}{B} Sorcery.
 *
 * "Each opponent gets a poison counter. Then each creature your opponents control gets -1/-1 until
 *  end of turn for each poison counter its controller has."
 *
 * Three seats, so each opponent's creatures shrink by *their own controller's* poison total (counted
 * after the new counter), and the caster's creatures are untouched. The reduction is locked in at
 * resolution (2023-02-04 ruling).
 */
class PhyresisOutbreakScenarioTest : FunSpec({

    fun GameTestDriver.poison(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("each opponent gets a poison counter and their creatures shrink by their own poison total, locked at resolution") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PhyresisOutbreak))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, heavilyPoisoned, clean) = players
        driver.addComponent(heavilyPoisoned, CountersComponent(mapOf(CounterType.POISON to 2)))

        val yourCreature = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val bigCreature = driver.putCreatureOnBattlefield(heavilyPoisoned, "Force of Nature")
        val cleanCreature = driver.putCreatureOnBattlefield(clean, "Centaur Courser")

        val spell = driver.putCardInHand(you, "Phyresis Outbreak")
        driver.giveMana(you, Color.BLACK, 3)
        driver.castSpell(you, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)

        withClue("each opponent got exactly one poison counter; the caster got none") {
            driver.poison(heavilyPoisoned) shouldBe 3
            driver.poison(clean) shouldBe 1
            driver.poison(you) shouldBe 0
        }

        val projected = driver.state.projectedState
        withClue("Force of Nature (5/5) gets -3/-3 — its controller now has three poison counters") {
            projected.getPower(bigCreature) shouldBe 2
            projected.getToughness(bigCreature) shouldBe 2
        }
        withClue("the other opponent's Centaur Courser (3/3) gets -1/-1 — its controller has one") {
            projected.getPower(cleanCreature) shouldBe 2
            projected.getToughness(cleanCreature) shouldBe 2
        }
        withClue("the caster's own creature is untouched") {
            projected.getPower(yourCreature) shouldBe 3
            projected.getToughness(yourCreature) shouldBe 3
        }

        // More poison later in the turn doesn't deepen the shrink.
        driver.addComponent(clean, CountersComponent(mapOf(CounterType.POISON to 4)))
        withClue("the reduction was fixed when Phyresis Outbreak resolved") {
            driver.state.projectedState.getPower(cleanCreature) shouldBe 2
            driver.state.projectedState.getToughness(cleanCreature) shouldBe 2
        }
    }
})
