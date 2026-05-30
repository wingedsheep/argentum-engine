package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the Invasion "Apprentice" cycle of 1/1 Human Wizards, each with two
 * colored-mana + tap activated abilities:
 *
 * - Sunscape Apprentice {W}: {G},{T}: +1/+1; {U},{T}: put target creature you control on top of library.
 * - Stormscape Apprentice {U}: {W},{T}: tap target creature; {B},{T}: target player loses 1 life.
 * - Thornscape Apprentice {G}: {R},{T}: grant first strike; {W},{T}: tap target creature.
 * - Thunderscape Apprentice {R}: {B},{T}: target player loses 1 life; {G},{T}: +1/+1.
 *
 * These cards introduce no new engine primitives — they compose existing facades
 * (ModifyStats, PutOnTopOfLibrary, TapUntapEffect, LoseLife, GrantKeyword). The tests
 * exercise one ability of each card to confirm wiring.
 */
class InvasionApprenticesScenarioTest : ScenarioTestBase() {

    private fun firstStrikeOn(name: String) =
        cardRegistry.getCard(name)!!.script.activatedAbilities

    init {
        context("Sunscape Apprentice") {
            test("{G},{T} gives target creature +1/+1 until end of turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Sunscape Apprentice")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(green = 1)) }

                val bears = game.findPermanent("Grizzly Bears")!!
                val source = game.findPermanent("Sunscape Apprentice")!!
                val ability = firstStrikeOn("Sunscape Apprentice")[0]

                val result = game.execute(
                    ActivateAbility(game.player1Id, source, ability.id, listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
            }
        }

        context("Stormscape Apprentice") {
            test("{B},{T} makes target player lose 1 life") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Stormscape Apprentice")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(black = 1)) }

                val source = game.findPermanent("Stormscape Apprentice")!!
                // ability[0] = {W} tap, ability[1] = {B} lose life
                val ability = firstStrikeOn("Stormscape Apprentice")[1]
                val startLife = game.state.getEntity(game.player2Id)?.get<LifeTotalComponent>()?.life ?: 0

                val result = game.execute(
                    ActivateAbility(game.player1Id, source, ability.id, listOf(ChosenTarget.Player(game.player2Id)))
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.state.getEntity(game.player2Id)?.get<LifeTotalComponent>()?.life shouldBe (startLife - 1)
            }
        }

        context("Thornscape Apprentice") {
            test("{R},{T} grants first strike until end of turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Thornscape Apprentice")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(red = 1)) }

                val bears = game.findPermanent("Grizzly Bears")!!
                val source = game.findPermanent("Thornscape Apprentice")!!
                val ability = firstStrikeOn("Thornscape Apprentice")[0]

                withClue("bears start without first strike") {
                    game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                }

                val result = game.execute(
                    ActivateAbility(game.player1Id, source, ability.id, listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            }
        }

        context("Thunderscape Apprentice") {
            test("{G},{T} gives target creature +1/+1 until end of turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Thunderscape Apprentice")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state = game.state.updateEntity(game.player1Id) { it.with(ManaPoolComponent(green = 1)) }

                val bears = game.findPermanent("Grizzly Bears")!!
                val source = game.findPermanent("Thunderscape Apprentice")!!
                // ability[0] = {B} lose life, ability[1] = {G} +1/+1
                val ability = firstStrikeOn("Thunderscape Apprentice")[1]

                val result = game.execute(
                    ActivateAbility(game.player1Id, source, ability.id, listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
            }
        }
    }
}
