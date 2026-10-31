package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Heliod's Punishment (THB) — enters with four task counters; the enchanted creature can't attack
 * or block, loses all abilities, and has "{T}: Remove a task counter from Heliod's Punishment. Then
 * if it has no task counters on it, destroy Heliod's Punishment."
 *
 * Covers the entering counters, the lock and ability loss, the countdown spent by the creature's
 * controller from the Aura's counters, and the release once the last counter goes.
 */
class HeliodsPunishmentScenarioTest : ScenarioTestBase() {

    private val taskAbilityId by lazy {
        cardRegistry.requireCard("Heliod's Punishment").script.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id
    }

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Heliod's Punishment")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, "Wind Drake")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.taskCounters(aura: EntityId): Int =
        state.getEntity(aura)?.get<CountersComponent>()?.getCount(CounterType.TASK) ?: 0

    init {
        test("enters with four task counters; the creature can't attack or block and loses its abilities") {
            val game = game()
            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Heliod's Punishment", drake).error shouldBe null
            game.resolveStack()

            val aura = game.findPermanent("Heliod's Punishment")!!
            game.taskCounters(aura) shouldBe 4
            game.state.projectedState.cantAttack(drake) shouldBe true
            game.state.projectedState.cantBlock(drake) shouldBe true
            withClue("the Drake lost flying") {
                game.state.projectedState.hasKeyword(drake, Keyword.FLYING) shouldBe false
            }
        }

        test("the creature's controller taps it to count down; the last counter destroys the Aura") {
            val game = game()
            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Heliod's Punishment", drake).error shouldBe null
            game.resolveStack()
            val aura = game.findPermanent("Heliod's Punishment")!!

            repeat(3) { i ->
                game.passPriority()
                game.execute(ActivateAbility(game.player2Id, drake, taskAbilityId)).error shouldBe null
                game.resolveStack()
                game.taskCounters(aura) shouldBe 3 - i
                game.findPermanent("Heliod's Punishment") shouldBe aura
                game.state = untapOrConsumeStun(game.state, drake).first
            }

            game.passPriority()
            game.execute(ActivateAbility(game.player2Id, drake, taskAbilityId)).error shouldBe null
            game.resolveStack()

            withClue("the fourth activation removed the last counter and destroyed the Aura") {
                game.findPermanent("Heliod's Punishment") shouldBe null
                game.isInGraveyard(1, "Heliod's Punishment") shouldBe true
            }
            withClue("the Drake is free again") {
                game.state.projectedState.cantAttack(drake) shouldBe false
                game.state.projectedState.hasKeyword(drake, Keyword.FLYING) shouldBe true
            }
        }

        test("the Aura's controller can't activate the granted ability of the opponent's creature") {
            val game = game()
            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Heliod's Punishment", drake).error shouldBe null
            game.resolveStack()

            game.execute(ActivateAbility(game.player1Id, drake, taskAbilityId)).error shouldNotBe null
            game.taskCounters(game.findPermanent("Heliod's Punishment")!!) shouldBe 4
        }

        test("under two Punishments only the most recently attached one counts down") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Heliod's Punishment")
                .withCardInHand(1, "Heliod's Punishment")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Wind Drake")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val drake = game.findPermanent("Wind Drake")!!
            game.castSpell(1, "Heliod's Punishment", drake).error shouldBe null
            game.resolveStack()
            val first = game.findPermanent("Heliod's Punishment")!!
            game.castSpell(1, "Heliod's Punishment", drake).error shouldBe null
            game.resolveStack()
            val second = game.state.getBattlefield().single {
                it != first && game.state.getEntity(it)?.get<CardComponent>()?.name == "Heliod's Punishment"
            }

            game.passPriority()
            game.execute(ActivateAbility(game.player2Id, drake, taskAbilityId)).error shouldBe null
            game.resolveStack()

            game.taskCounters(second) shouldBe 3
            game.taskCounters(first) shouldBe 4
        }
    }
}
