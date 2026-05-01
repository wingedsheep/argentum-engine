package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Abigale, Eloquent First-Year (ECL).
 *
 * {W/B}{W/B} Legendary Creature — Bird Bard, 1/1.
 * - Flying, first strike, lifelink.
 * - When Abigale enters, up to one other target creature loses all abilities.
 *   Put a flying counter, a first strike counter, and a lifelink counter on that creature.
 *
 * Per Wizards rulings, the "loses all abilities" rider lasts indefinitely (it doesn't
 * end at cleanup or when Abigale leaves). Flying / first strike / lifelink counters
 * grant their respective keywords back via Rule 122.1b.
 */
class AbigaleEloquentFirstYearScenarioTest : ScenarioTestBase() {

    init {
        context("Abigale ETB — strip abilities, grant keyword counters") {

            test("targets a creature: it loses all abilities and gains the three keyword counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Abigale, Eloquent First-Year")
                    // Cloud Pirates — 1/1 flyer with a static ability ("can only block
                    // creatures with flying"). Both abilities should be stripped.
                    .withCardOnBattlefield(2, "Cloud Pirates")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Abigale, Eloquent First-Year")
                game.resolveStack()

                withClue("Abigale's ETB should pause for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                val piratesId = game.findPermanent("Cloud Pirates")!!
                game.selectTargets(listOf(piratesId))
                game.resolveStack()

                val projected = game.state.projectedState

                withClue("Cloud Pirates should have lost all of its base abilities") {
                    projected.hasLostAllAbilities(piratesId) shouldBe true
                }

                val counters = game.state.getEntity(piratesId)?.get<CountersComponent>()
                withClue("Pirates should have flying / first strike / lifelink counters") {
                    counters?.getCount(CounterType.FLYING) shouldBe 1
                    counters?.getCount(CounterType.FIRST_STRIKE) shouldBe 1
                    counters?.getCount(CounterType.LIFELINK) shouldBe 1
                }

                withClue("Keyword counters should grant the matching keywords back via Rule 122.1b") {
                    projected.hasKeyword(piratesId, "FLYING") shouldBe true
                    projected.hasKeyword(piratesId, "FIRST_STRIKE") shouldBe true
                    projected.hasKeyword(piratesId, "LIFELINK") shouldBe true
                }
            }

            test("ability is optional — player can skip target and Abigale still ETBs") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Abigale, Eloquent First-Year")
                    .withCardOnBattlefield(2, "Cloud Pirates")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Abigale, Eloquent First-Year")
                game.resolveStack()

                withClue("ETB should pause for the optional target") {
                    game.hasPendingDecision() shouldBe true
                }

                game.skipTargets()
                game.resolveStack()

                val piratesId = game.findPermanent("Cloud Pirates")!!
                val projected = game.state.projectedState

                withClue("Pirates' abilities must be untouched when no target is chosen") {
                    projected.hasLostAllAbilities(piratesId) shouldBe false
                    projected.hasKeyword(piratesId, "FLYING") shouldBe true
                }

                val piratesCounters = game.state.getEntity(piratesId)?.get<CountersComponent>()
                withClue("Pirates should have no keyword counters when no target was chosen") {
                    (piratesCounters?.getCount(CounterType.FLYING) ?: 0) shouldBe 0
                    (piratesCounters?.getCount(CounterType.FIRST_STRIKE) ?: 0) shouldBe 0
                    (piratesCounters?.getCount(CounterType.LIFELINK) ?: 0) shouldBe 0
                }

                withClue("Abigale should still be on the battlefield with its own keywords") {
                    game.isOnBattlefield("Abigale, Eloquent First-Year") shouldBe true
                    val abigaleId = game.findPermanent("Abigale, Eloquent First-Year")!!
                    projected.hasKeyword(abigaleId, "FLYING") shouldBe true
                    projected.hasKeyword(abigaleId, "FIRST_STRIKE") shouldBe true
                    projected.hasKeyword(abigaleId, "LIFELINK") shouldBe true
                }
            }

            test("excludes self — Abigale cannot target itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Abigale, Eloquent First-Year")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Abigale, Eloquent First-Year")
                game.resolveStack()

                val abigaleId = game.findPermanent("Abigale, Eloquent First-Year")!!

                withClue("ETB pauses for the optional 'up to one other target' selection") {
                    game.hasPendingDecision() shouldBe true
                }

                val result = game.selectTargets(listOf(abigaleId))
                withClue("Selecting Abigale herself must be rejected (excludeSelf)") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
