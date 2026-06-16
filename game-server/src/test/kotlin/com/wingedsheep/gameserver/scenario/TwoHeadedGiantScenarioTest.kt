package com.wingedsheep.gameserver.scenario

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Two-Headed Giant — Phase 6 hotseat path. A scenario request may carry a [ScenarioRequest.teams]
 * partition; [ScenarioBuilderService] then stamps the engine team model (TeamComponent + the 2HG
 * format) so a single-client hotseat can drive a faithful 2HG game for manual testing before the
 * lobby UI lands. Teams are seats 0+1 vs 2+3.
 */
class TwoHeadedGiantScenarioTest : ScenarioTestBase() {

    private val service get() = ScenarioBuilderService(cardRegistry)

    private fun fourSeat(teams: List<List<Int>>? = listOf(listOf(0, 1), listOf(2, 3))) =
        ScenarioRequest(
            players = listOf(
                ScenarioSeat("A1", PlayerConfig(lifeTotal = 30)),
                ScenarioSeat("A2", PlayerConfig(lifeTotal = 20)),
                ScenarioSeat("B1", PlayerConfig(lifeTotal = 30)),
                ScenarioSeat("B2", PlayerConfig(lifeTotal = 20)),
            ),
            teams = teams,
            mode = ScenarioMode.SELF,
        )

    init {
        test("teams partition stamps TeamComponent on every seat and runs under the 2HG format") {
            val build = service.buildScenario(fourSeat())
            val s = build.state

            s.format.shouldBeInstanceOf<Format.TwoHeadedGiant>()
            build.playerIds.map {
                s.getEntity(it)?.get<TeamComponent>()?.teamIndex
            } shouldContainExactly listOf(0, 0, 1, 1)
        }

        test("life resolves to the shared team total: a teammate reads the canonical owner") {
            val build = service.buildScenario(fourSeat())
            val s = build.state
            val a1 = EntityId.of("player-1") // team 0 canonical owner (life 30)
            val a2 = EntityId.of("player-2") // team 0 teammate, own raw component 20

            // The teammate's own raw component is 20, but the shared total reads the canonical owner.
            s.getEntity(a2)?.get<LifeTotalComponent>()?.life shouldBe 20
            s.lifeTotal(a1) shouldBe 30
            s.lifeTotal(a2) shouldBe 30
        }

        test("without teams the scenario is unchanged: no TeamComponent, default format") {
            val build = service.buildScenario(fourSeat(teams = null))
            val s = build.state
            build.playerIds.forEach { s.getEntity(it)?.get<TeamComponent>() shouldBe null }
            s.format.shouldBeInstanceOf<Format.Standard>()
        }

        test("validate accepts a well-formed 4-seat 2HG hotseat request") {
            service.validate(fourSeat(), enforceLimits = true) shouldHaveSize 0
        }

        test("validate rejects a teams list that does not partition every seat") {
            // Seat 3 is missing and seat 1 is doubled — not a clean partition of [0,1,2,3].
            val errors = service.validate(fourSeat(teams = listOf(listOf(0, 1), listOf(1, 2))), enforceLimits = true)
            errors.any { it.contains("partition") } shouldBe true
        }
    }
}
