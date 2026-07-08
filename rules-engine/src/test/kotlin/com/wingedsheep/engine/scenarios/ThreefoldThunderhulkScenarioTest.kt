package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.ThreefoldThunderhulk
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Threefold Thunderhulk (LCI #265) — {7} Artifact Creature — Gnome 0/0.
 *
 * "This creature enters with three +1/+1 counters on it.
 *  Whenever this creature enters or attacks, create a number of 1/1 colorless Gnome artifact
 *    creature tokens equal to its power.
 *  {2}, Sacrifice another artifact: Put a +1/+1 counter on this creature."
 *
 * Exercises:
 *  - [EntersWithCounters] replacement: the Thunderhulk arrives with three +1/+1 counters, making
 *    it a 3/3 on entry.
 *  - Enters trigger: creating tokens equal to its power (3) as it enters.
 *  - Attacks trigger: creating tokens equal to its power when it attacks (post-pump count).
 *  - Activated ability: {2}, Sacrifice another artifact → one +1/+1 counter on the Thunderhulk.
 */
class ThreefoldThunderhulkScenarioTest : ScenarioTestBase() {

    init {
        // Threefold Thunderhulk is auto-discovered from the LCI cards package, so it is already
        // present in the shared cardRegistry. No explicit registration needed.
        val activateAbilityId = ThreefoldThunderhulk.activatedAbilities.first().id
        val projector = StateProjector()

        fun plusOne(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        /** Directly add +1/+1 counters to a battlefield entity without going through the stack. */
        fun addPlusOne(game: TestGame, id: EntityId, count: Int) {
            game.state = game.state.updateEntity(id) { container ->
                val existing = container.get<CountersComponent>() ?: CountersComponent()
                container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
            }
        }

        /** All Gnome tokens controlled by player 1. */
        fun gnomeTokens(game: TestGame): List<EntityId> =
            game.state.getBattlefield().filter { id ->
                val e = game.state.getEntity(id) ?: return@filter false
                e.has<TokenComponent>() && e.get<CardComponent>()?.name == "Gnome Token"
            }

        context("Threefold Thunderhulk") {

            test("enters with three +1/+1 counters and creates three Gnome tokens equal to its power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threefold Thunderhulk")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Threefold Thunderhulk").error shouldBe null
                game.resolveStack()

                val hulk = game.findPermanent("Threefold Thunderhulk")!!
                withClue("enters with three +1/+1 counters (0/0 base → 3/3)") {
                    plusOne(game, hulk) shouldBe 3
                    projector.getProjectedPower(game.state, hulk) shouldBe 3
                }
                withClue("enters trigger creates tokens equal to its power (3)") {
                    gnomeTokens(game).size shouldBe 3
                }

                // Each token is a 1/1 colorless Gnome artifact creature.
                val token = gnomeTokens(game).first()
                val card = game.state.getEntity(token)!!.get<CardComponent>()!!
                card.typeLine.isArtifact shouldBe true
                card.typeLine.isCreature shouldBe true
                card.typeLine.subtypes.map { it.value } shouldBe listOf("Gnome")
                card.colors shouldBe emptySet()
                projector.getProjectedPower(game.state, token) shouldBe 1
                projector.getProjectedToughness(game.state, token) shouldBe 1
            }

            test("attacks trigger creates tokens equal to its power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Threefold Thunderhulk")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hulk = game.findPermanent("Threefold Thunderhulk")!!
                // Placed directly (no ETB replacement), so simulate the three entry counters → 3/3.
                addPlusOne(game, hulk, 3)
                withClue("no tokens before combat") { gnomeTokens(game).size shouldBe 0 }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Threefold Thunderhulk" to 2))
                game.resolveStack()

                withClue("attacks trigger creates tokens equal to its power (3)") {
                    gnomeTokens(game).size shouldBe 3
                }
            }

            test("{2}, Sacrifice another artifact: put a +1/+1 counter on this creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Threefold Thunderhulk")
                    .withCardOnBattlefield(1, "Artifact Creature")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hulk = game.findPermanent("Threefold Thunderhulk")!!
                addPlusOne(game, hulk, 3)
                val fodder = game.findPermanent("Artifact Creature")!!

                withClue("three counters before activation") { plusOne(game, hulk) shouldBe 3 }

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = hulk, abilityId = activateAbilityId)
                )
                result.error shouldBe null
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Thunderhulk gains a +1/+1 counter (3 → 4)") {
                    plusOne(game, hulk) shouldBe 4
                }
                withClue("the other artifact was sacrificed") {
                    game.state.getBattlefield().contains(fodder) shouldBe false
                }
            }
        }
    }
}
