package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.ExplorersCache
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Explorer's Cache (LCI #184).
 *
 * "{1}{G} Artifact
 *  This artifact enters with two +1/+1 counters on it.
 *  Whenever a creature you control with a +1/+1 counter on it dies, put a +1/+1 counter on
 *    this artifact.
 *  {T}: Move a +1/+1 counter from this artifact onto target creature. Activate only as a sorcery."
 *
 * Exercises:
 *  - [EntersWithCounters] replacement: Cache arrives with exactly two +1/+1 counters.
 *  - LKI-filtered dies trigger: fires when a creature you control that had a +1/+1 counter dies;
 *    does NOT fire when the dying creature had no +1/+1 counter.
 *  - [Effects.MoveCounters] activated ability: moves one +1/+1 counter from the Cache to a target
 *    creature; the source loses the counter and the target gains it.
 *  - [TimingRule.SorcerySpeed] gate: the tap ability is rejected outside a main phase.
 */
class ExplorersCacheScenarioTest : ScenarioTestBase() {

    init {
        // Explorer's Cache is auto-discovered from the LCI cards package and therefore already
        // present in TestCards.all / cardRegistry. No explicit re-registration needed.

        val abilityId = ExplorersCache.activatedAbilities[0].id

        fun plusOne(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        /** Directly add +1/+1 counters to a battlefield entity without going through the stack. */
        fun addPlusOne(game: TestGame, id: EntityId, count: Int) {
            game.state = game.state.updateEntity(id) { container ->
                val existing = container.get<CountersComponent>() ?: CountersComponent()
                container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, count))
            }
        }

        context("Explorer's Cache") {

            test("enters with two +1/+1 counters via replacement effect") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Explorer's Cache")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Explorer's Cache").error shouldBe null
                game.resolveStack()

                val cache = game.findPermanent("Explorer's Cache")!!
                withClue("Explorer's Cache enters with two +1/+1 counters") {
                    plusOne(game, cache) shouldBe 2
                }
            }

            test("dies trigger fires when a creature you control with a +1/+1 counter dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Explorer's Cache")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardsInHand(1, "Lightning Bolt", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast the Cache so it enters with two +1/+1 counters via the replacement effect.
                game.castSpell(1, "Explorer's Cache").error shouldBe null
                game.resolveStack()

                val cache = game.findPermanent("Explorer's Cache")!!
                val bear = game.findPermanent("Grizzly Bears")!!
                withClue("Cache has two counters") { plusOne(game, cache) shouldBe 2 }

                // Add a +1/+1 counter to the Grizzly Bears (2/2 + counter = 3/3, dies to bolt).
                addPlusOne(game, bear, 1)
                withClue("Bear has a +1/+1 counter before dying") { plusOne(game, bear) shouldBe 1 }

                // Lightning Bolt deals 3 damage — enough to kill the 3/3 bear.
                game.castSpell(1, "Lightning Bolt", bear).error shouldBe null
                game.resolveStack()

                withClue("Bear is dead") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("Cache gains a +1/+1 counter because a creature with a +1/+1 counter died") {
                    plusOne(game, cache) shouldBe 3
                }
            }

            test("dies trigger does NOT fire when a creature you control without a +1/+1 counter dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Explorer's Cache")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardsInHand(1, "Lightning Bolt", 1)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Explorer's Cache").error shouldBe null
                game.resolveStack()

                val cache = game.findPermanent("Explorer's Cache")!!
                val bear = game.findPermanent("Grizzly Bears")!!
                withClue("Cache has two counters") { plusOne(game, cache) shouldBe 2 }
                withClue("Bear has no +1/+1 counter") { plusOne(game, bear) shouldBe 0 }

                // Bolt kills the 2/2 Grizzly Bears which has no +1/+1 counter.
                game.castSpell(1, "Lightning Bolt", bear).error shouldBe null
                game.resolveStack()

                withClue("Bear is dead") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("Cache counter count is unchanged — trigger did not fire") {
                    plusOne(game, cache) shouldBe 2
                }
            }

            test("{T}: move a +1/+1 counter from the Cache onto a target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Explorer's Cache")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cache = game.findPermanent("Explorer's Cache")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                // Seed the Cache with two +1/+1 counters (simulating the ETB replacement).
                addPlusOne(game, cache, 2)
                withClue("Cache has two counters before activation") { plusOne(game, cache) shouldBe 2 }
                withClue("Bear has no counters before activation") { plusOne(game, bear) shouldBe 0 }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cache,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                result.error shouldBe null
                game.resolveStack()

                withClue("Cache loses one +1/+1 counter") { plusOne(game, cache) shouldBe 1 }
                withClue("Bear gains one +1/+1 counter") { plusOne(game, bear) shouldBe 1 }
            }

            test("activated ability cannot be used outside a main phase (sorcery-speed gate)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Explorer's Cache")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val cache = game.findPermanent("Explorer's Cache")!!
                val bear = game.findPermanent("Grizzly Bears")!!
                addPlusOne(game, cache, 2)

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cache,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bear))
                    )
                )
                withClue("Sorcery-speed activation is rejected during combat") {
                    result.error shouldBe "This ability can only be activated as a sorcery"
                }
            }
        }
    }
}
