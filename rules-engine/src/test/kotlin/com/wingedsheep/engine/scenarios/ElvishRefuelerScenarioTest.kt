package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for Elvish Refueler — "During your turn, as long as you haven't activated an
 * exhaust ability this turn, you may activate exhaust abilities as though they haven't been
 * activated."
 *
 * The net effect the claims below pin down: once per turn, on your own turn, you may re-use one
 * already-spent exhaust ability — and the re-use itself burns the permission for the rest of that
 * turn.
 *
 *  - the turn's first exhaust activation consumes the waiver, so a same-turn repeat is still illegal;
 *  - on a later turn of yours the per-turn count has reset, so an already-spent exhaust ability of
 *    *any* permanent you control is activatable again;
 *  - the waiver never applies on an opponent's turn;
 *  - it doesn't reach a plain [ActivationRestriction.Once] on a non-exhaust ability.
 */
class ElvishRefuelerScenarioTest : ScenarioTestBase() {

    // A second exhaust body, so we can prove the waiver isn't scoped to the Refueler's own ability.
    private val exhaustBuddy = card("Exhaust Buddy") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        activatedAbility {
            isExhaust = true
            cost = Costs.Mana("{1}")
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // Non-exhaust, but printed "Activate only once" — the waiver must not reach it.
    private val onceOnly = card("Once Only") {
        manaCost = "{2}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            restrictions = listOf(ActivationRestriction.Once)
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    private val refuelerAbility
        get() = cardRegistry.getCard("Elvish Refueler")!!.script.activatedAbilities.single { it.isExhaust }
    private val buddyAbility get() = cardRegistry.getCard("Exhaust Buddy")!!.script.activatedAbilities.single()
    private val onceOnlyAbility get() = cardRegistry.getCard("Once Only")!!.script.activatedAbilities.single()

    private fun TestGame.activateAndResolve(permanent: EntityId, abilityId: com.wingedsheep.sdk.scripting.AbilityId) =
        execute(ActivateAbility(player1Id, permanent, abilityId)).also { result ->
            if (result.error == null) {
                if (state.pendingDecision is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
            }
        }

    /** Pass priority through one whole turn, landing in the next turn's precombat main phase. */
    private fun TestGame.advanceOneTurn() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
        passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    private fun TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(extraCard: String) = run {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Elvish Refueler", summoningSickness = false)
            .withCardOnBattlefield(1, extraCard, summoningSickness = false)
            .withLandsOnBattlefield(1, "Forest", 6)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
        repeat(10) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
        builder.build()
    }

    init {
        cardRegistry.register(exhaustBuddy)
        cardRegistry.register(onceOnly)

        test("the turn's first exhaust activation consumes the waiver") {
            val game = board("Exhaust Buddy")
            val refueler = game.findPermanent("Elvish Refueler")!!

            game.activateAndResolve(refueler, refuelerAbility.id).error shouldBe null
            game.plusOneCounters(refueler) shouldBe 1

            val second = game.execute(ActivateAbility(game.player1Id, refueler, refuelerAbility.id))
            withClue("you have now activated an exhaust ability this turn, so the waiver is gone") {
                (second.error != null) shouldBe true
            }
            game.plusOneCounters(refueler) shouldBe 1
        }

        test("on a later turn of yours, another permanent's spent exhaust ability is re-openable") {
            val game = board("Exhaust Buddy")
            val buddy = game.findPermanent("Exhaust Buddy")!!

            game.activateAndResolve(buddy, buddyAbility.id).error shouldBe null
            game.plusOneCounters(buddy) shouldBe 1
            withClue("still the same turn — the waiver is already spent") {
                (game.execute(ActivateAbility(game.player1Id, buddy, buddyAbility.id)).error != null) shouldBe true
            }

            // Opponent's turn, then back to ours: the per-turn exhaust count has reset.
            game.advanceOneTurn()
            game.advanceOneTurn()
            game.state.activePlayerId shouldBe game.player1Id

            val reactivate = game.activateAndResolve(buddy, buddyAbility.id)
            withClue("Elvish Refueler should re-open the Buddy's exhaust ability: ${reactivate.error}") {
                reactivate.error shouldBe null
            }
            game.plusOneCounters(buddy) shouldBe 2
        }

        test("the waiver does not apply on an opponent's turn") {
            val game = board("Exhaust Buddy")
            val buddy = game.findPermanent("Exhaust Buddy")!!

            game.activateAndResolve(buddy, buddyAbility.id).error shouldBe null

            // One turn on: the exhaust count has reset, but it is not our turn.
            game.advanceOneTurn()
            game.state.activePlayerId shouldBe game.player2Id

            val onOpponentsTurn = game.execute(ActivateAbility(game.player1Id, buddy, buddyAbility.id))
            withClue("the permission is limited to your own turn") {
                (onOpponentsTurn.error != null) shouldBe true
            }
            game.plusOneCounters(buddy) shouldBe 1
        }

        test("a non-exhaust 'activate only once' ability is unaffected") {
            val game = board("Once Only")
            val once = game.findPermanent("Once Only")!!

            game.activateAndResolve(once, onceOnlyAbility.id).error shouldBe null
            game.plusOneCounters(once) shouldBe 1

            // No exhaust ability has been activated this turn, so the waiver is fully in force —
            // it still must not reach a plain Once restriction on a non-exhaust ability.
            val second = game.execute(ActivateAbility(game.player1Id, once, onceOnlyAbility.id))
            withClue("the waiver is scoped to exhaust abilities only") {
                (second.error != null) shouldBe true
            }
            game.plusOneCounters(once) shouldBe 1
        }
    }
}
