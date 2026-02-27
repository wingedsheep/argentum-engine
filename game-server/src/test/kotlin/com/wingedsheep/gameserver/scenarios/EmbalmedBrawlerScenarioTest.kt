package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Embalmed Brawler — tests the Amplify mechanic and the
 * "whenever attacks or blocks, lose life" triggered ability.
 *
 * Card reference:
 * - Embalmed Brawler (2B): 2/2 Creature — Zombie
 *   Amplify 1
 *   Whenever Embalmed Brawler attacks or blocks, you lose 1 life for each +1/+1 counter on it.
 */
class EmbalmedBrawlerScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun ScenarioTestBase.TestGame.getCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Embalmed Brawler Amplify") {
            test("enters with +1/+1 counters when revealing a Zombie from hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embalmed Brawler")
                    .withCardInHand(1, "Withered Wretch") // Zombie in hand to reveal
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Embalmed Brawler
                game.castSpell(1, "Embalmed Brawler")
                // Pass priority to start resolution — this should trigger the Amplify decision
                game.passPriority() // Player 1 passes
                game.passPriority() // Player 2 passes — spell starts resolving

                // Should have a pending decision for Amplify card reveal
                withClue("Should have pending Amplify decision") {
                    game.state.pendingDecision shouldNotBe null
                }

                // Find the Withered Wretch in hand and reveal it
                val wretchId = game.findCardsInHand(1, "Withered Wretch")
                wretchId.size shouldBe 1

                game.selectCards(wretchId)

                // Resolve any remaining stack items
                game.resolveStack()

                // Embalmed Brawler should be on the battlefield with 1 counter
                val brawler = game.findPermanent("Embalmed Brawler")!!
                withClue("Embalmed Brawler should have 1 +1/+1 counter from Amplify") {
                    game.getCounters(brawler) shouldBe 1
                }

                // P/T should be 3/3 (2/2 base + 1 counter)
                val projected = stateProjector.project(game.state)
                withClue("Embalmed Brawler should be 3/3 with 1 counter") {
                    projected.getPower(brawler) shouldBe 3
                    projected.getToughness(brawler) shouldBe 3
                }
            }

            test("enters without counters when no matching creatures in hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embalmed Brawler")
                    .withCardInHand(1, "Grizzly Bears") // Not a Zombie
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Embalmed Brawler — no Zombies in hand, so no Amplify decision
                game.castSpell(1, "Embalmed Brawler")
                game.resolveStack()

                // Embalmed Brawler should be on the battlefield with no counters
                val brawler = game.findPermanent("Embalmed Brawler")!!
                withClue("Embalmed Brawler should have 0 counters (no Zombies to reveal)") {
                    game.getCounters(brawler) shouldBe 0
                }

                val projected = stateProjector.project(game.state)
                withClue("Embalmed Brawler should be 2/2 with no counters") {
                    projected.getPower(brawler) shouldBe 2
                    projected.getToughness(brawler) shouldBe 2
                }
            }

            test("can choose to reveal zero cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embalmed Brawler")
                    .withCardInHand(1, "Withered Wretch") // Zombie available but player declines
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Embalmed Brawler
                game.castSpell(1, "Embalmed Brawler")
                game.passPriority()
                game.passPriority()

                // Amplify decision should be pending — select zero cards
                game.selectCards(emptyList())
                game.resolveStack()

                // Should enter with no counters
                val brawler = game.findPermanent("Embalmed Brawler")!!
                withClue("Embalmed Brawler should have 0 counters when declining to reveal") {
                    game.getCounters(brawler) shouldBe 0
                }
            }

            test("can reveal multiple Zombies for multiple counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embalmed Brawler")
                    .withCardInHand(1, "Withered Wretch") // Zombie 1
                    .withCardInHand(1, "Dripping Dead") // Zombie 2
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Embalmed Brawler
                game.castSpell(1, "Embalmed Brawler")
                game.passPriority()
                game.passPriority()

                // Reveal both Zombies
                val wretchIds = game.findCardsInHand(1, "Withered Wretch")
                val drippingDeadIds = game.findCardsInHand(1, "Dripping Dead")
                game.selectCards(wretchIds + drippingDeadIds)
                game.resolveStack()

                // Should have 2 counters
                val brawler = game.findPermanent("Embalmed Brawler")!!
                withClue("Embalmed Brawler should have 2 +1/+1 counters from revealing 2 Zombies") {
                    game.getCounters(brawler) shouldBe 2
                }

                val projected = stateProjector.project(game.state)
                withClue("Embalmed Brawler should be 4/4 with 2 counters") {
                    projected.getPower(brawler) shouldBe 4
                    projected.getToughness(brawler) shouldBe 4
                }
            }
        }

        context("Embalmed Brawler attacks/blocks life loss") {
            test("loses life equal to counters when attacking") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Embalmed Brawler")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually add 2 +1/+1 counters to Embalmed Brawler
                val brawlerId = game.findPermanent("Embalmed Brawler")!!
                game.state = game.state.updateEntity(brawlerId) { c ->
                    c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
                }

                val initialLife = game.getLifeTotal(1)
                initialLife shouldBe 20

                // Advance to combat and declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Embalmed Brawler" to 2))

                // Resolve the "attacks" triggered ability
                game.resolveStack()

                // Player 1 should have lost 2 life (1 per +1/+1 counter)
                withClue("Player should lose 2 life when attacking with 2 +1/+1 counters") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("no life loss when no counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Embalmed Brawler") // No counters
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Advance to combat and declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Embalmed Brawler" to 2))

                // Resolve the trigger (it will try to lose 0 life)
                game.resolveStack()

                // Life should be unchanged
                withClue("Player should lose 0 life when attacking with no counters") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
