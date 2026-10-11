package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gourmand's Talent {G} — Enchantment — Class
 *   During your turn, artifacts you control are Foods in addition to their other types and have
 *   "{2}, {T}, Sacrifice this artifact: You gain 3 life."
 *   {2}{G}: Level 2 — Whenever you gain life for the first time each turn, create a 3/3 green
 *   Raccoon creature token.
 *   {3}{G}: Level 3 — Whenever you gain life for the first time each turn, put a +1/+1 counter on
 *   each creature you control.
 *
 * What the snapshot can't see: both halves of the level-1 grant are gated on whose turn it is, and
 * the level 2/3 triggers fire only on the first life gain of the turn.
 */
class GourmandsTalentScenarioTest : ScenarioTestBase() {

    private val foodAbilityId: AbilityId by lazy {
        cardRegistry.getCard("Gourmand's Talent")!!.staticAbilities
            .mapNotNull { (it as? ConditionalStaticAbility)?.ability as? GrantActivatedAbility }
            .single().ability.id
    }

    private fun gameWith(level: Int, activePlayer: Int, ornithopters: Int, forests: Int, bears: Boolean = false) =
        scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Gourmand's Talent", classLevel = level)
            .apply { repeat(ornithopters) { withCardOnBattlefield(1, "Ornithopter") } }
            .apply { if (bears) withCardOnBattlefield(1, "Grizzly Bears") }
            .withLandsOnBattlefield(1, "Forest", forests)
            .withActivePlayer(activePlayer)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(5) { withCardInLibrary(1, "Grizzly Bears") } }
            .apply { repeat(5) { withCardInLibrary(2, "Grizzly Bears") } }
            .build()

    /** Resolve everything, putting simultaneous triggers on the stack in the offered order. */
    private fun TestGame.settle() {
        repeat(20) {
            resolveStack()
            when (val decision = getPendingDecision()) {
                is OrderObjectsDecision ->
                    submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
                null -> if (state.stack.isEmpty()) return
                else -> error("unexpected decision: $decision")
            }
        }
    }

    /** Eat [artifact] through the granted ability and resolve everything. */
    private fun TestGame.eat(artifact: EntityId) {
        val result = execute(ActivateAbility(playerId = player1Id, sourceId = artifact, abilityId = foodAbilityId))
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        if (getPendingDecision() != null && getPendingDecision() !is OrderObjectsDecision) submitManaSourcesAutoPay()
        settle()
    }

    private fun TestGame.raccoons(): List<EntityId> =
        state.getBattlefield().filter { id ->
            val c = state.getEntity(id) ?: return@filter false
            c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == player1Id &&
                state.projectedState.hasSubtype(id, "Raccoon")
        }

    init {
        test("during your turn, your artifacts are Foods and can be eaten for 3 life") {
            val game = gameWith(level = 1, activePlayer = 1, ornithopters = 1, forests = 2)
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.state.projectedState.hasSubtype(ornithopter, "Food") shouldBe true

            game.eat(ornithopter)

            game.getLifeTotal(1) shouldBe 23
            game.isInGraveyard(1, "Ornithopter") shouldBe true
            withClue("level 1 has no life-gain trigger") { game.raccoons().size shouldBe 0 }
        }

        test("during an opponent's turn, your artifacts are neither Foods nor edible") {
            val game = gameWith(level = 1, activePlayer = 2, ornithopters = 1, forests = 2)
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.state.projectedState.hasSubtype(ornithopter, "Food") shouldBe false
            val canEat = game.getLegalActions(1).any {
                val action = it.action as? ActivateAbility
                action?.sourceId == ornithopter && action.abilityId == foodAbilityId
            }
            withClue("the granted ability exists only during your turn") { canEat shouldBe false }
        }

        test("level 2: only the first life gain of the turn makes a 3/3 green Raccoon") {
            val game = gameWith(level = 2, activePlayer = 1, ornithopters = 2, forests = 4)
            val (first, second) = game.findPermanents("Ornithopter")

            game.eat(first)
            game.eat(second)

            game.getLifeTotal(1) shouldBe 26
            val raccoons = game.raccoons()
            withClue("exactly one Raccoon across two life gains") { raccoons.size shouldBe 1 }
            val raccoon = raccoons.single()
            game.state.projectedState.getPower(raccoon) shouldBe 3
            game.state.projectedState.getToughness(raccoon) shouldBe 3
            game.state.projectedState.hasColor(raccoon, Color.GREEN) shouldBe true
        }

        test("level 3: the first life gain puts a +1/+1 counter on each creature you control, once") {
            val game = gameWith(level = 3, activePlayer = 1, ornithopters = 2, forests = 4, bears = true)
            val bears = game.findPermanent("Grizzly Bears")!!
            val (first, second) = game.findPermanents("Ornithopter")

            game.eat(first)
            game.eat(second)

            val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            withClue("one counter from the first life gain, none from the second") { counters shouldBe 1 }
            withClue("the level-2 ability still triggers once at level 3") { game.raccoons().size shouldBe 1 }
        }
    }
}
