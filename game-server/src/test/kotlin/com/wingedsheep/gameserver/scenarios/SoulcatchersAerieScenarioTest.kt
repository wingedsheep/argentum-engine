package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Soulcatchers' Aerie.
 *
 * Card reference:
 * - Soulcatchers' Aerie ({1}{W}): Enchantment
 *   "Whenever a Bird is put into your graveyard from the battlefield, put a feather
 *    counter on this enchantment.
 *    Bird creatures get +1/+1 for each feather counter on this enchantment."
 *
 * Exercises the new FEATHER counter type plus the existing
 * `Triggers.leavesBattlefield` + `GrantDynamicStatsEffect` building blocks.
 */
class SoulcatchersAerieScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    // Vanilla Birds — no abilities of their own, so only the Aerie touches their stats.
    private val testSparrow = CardDefinition.creature(
        name = "Test Sparrow",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.BIRD),
        power = 1,
        toughness = 1
    )

    // A distinct Bird so the opponent's copy is addressable by name.
    private val testFalcon = CardDefinition.creature(
        name = "Test Falcon",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype.BIRD),
        power = 1,
        toughness = 1
    )

    init {
        cardRegistry.register(testSparrow)
        cardRegistry.register(testFalcon)

        context("Soulcatchers' Aerie") {

            test("a Bird you control dying adds a feather counter and buffs all Birds") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Soulcatchers' Aerie")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardOnBattlefield(1, "Test Sparrow") // sacrificed
                    .withCardOnBattlefield(1, "Test Sparrow") // survivor
                    .withCardOnBattlefield(2, "Test Falcon")  // opponent's Bird (also buffed)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerieId = game.findPermanent("Soulcatchers' Aerie")!!
                val huskId = game.findPermanent("Nantuko Husk")!!
                val sparrows = game.findPermanents("Test Sparrow")
                val sacrificed = sparrows[0]
                val survivor = sparrows[1]
                val opponentBird = game.findPermanent("Test Falcon")!!

                withClue("No feather counters yet, so no buff") {
                    featherCount(game, aerieId) shouldBe 0
                    stateProjector.project(game.state).getPower(survivor) shouldBe 1
                }

                val huskAbility = cardRegistry.getCard("Nantuko Husk")!!.script.activatedAbilities.first()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huskId,
                        abilityId = huskAbility.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(sacrificed))
                    )
                )
                withClue("Sacrificing a Bird to Nantuko Husk should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Aerie trigger (add feather counter) + Husk's +2/+2 ability resolve.
                game.resolveStack()

                withClue("Aerie should have 1 feather counter") {
                    featherCount(game, aerieId) shouldBe 1
                }

                val projected = stateProjector.project(game.state)
                withClue("Surviving Bird you control should be 2/2 (base 1/1 +1/+1)") {
                    projected.getPower(survivor) shouldBe 2
                    projected.getToughness(survivor) shouldBe 2
                }
                withClue("Opponent's Bird should also be buffed (lord is symmetric)") {
                    projected.getPower(opponentBird) shouldBe 2
                    projected.getToughness(opponentBird) shouldBe 2
                }
            }

            test("a non-Bird dying does not add a feather counter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Soulcatchers' Aerie")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 Human Soldier, not a Bird
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerieId = game.findPermanent("Soulcatchers' Aerie")!!
                val huskId = game.findPermanent("Nantuko Husk")!!
                val glorySeeker = game.findPermanent("Glory Seeker")!!

                val huskAbility = cardRegistry.getCard("Nantuko Husk")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huskId,
                        abilityId = huskAbility.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(glorySeeker))
                    )
                )
                game.resolveStack()

                withClue("Sacrificing a non-Bird should not add a feather counter") {
                    featherCount(game, aerieId) shouldBe 0
                }
            }

            test("an opponent's Bird dying does not add a feather counter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Soulcatchers' Aerie")
                    .withCardOnBattlefield(2, "Nantuko Husk")
                    .withCardOnBattlefield(2, "Test Sparrow")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerieId = game.findPermanent("Soulcatchers' Aerie")!!
                val huskId = game.findPermanent("Nantuko Husk")!!
                val opponentBird = game.findPermanent("Test Sparrow")!!

                val huskAbility = cardRegistry.getCard("Nantuko Husk")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = huskId,
                        abilityId = huskAbility.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(opponentBird))
                    )
                )
                game.resolveStack()

                withClue("The Aerie only cares about Birds put into ITS controller's graveyard") {
                    featherCount(game, aerieId) shouldBe 0
                }
            }

            // Edge case (CR 400.3): "your graveyard" is ownership, not control. A permanent
            // always goes to its owner's graveyard.
            test("a Bird you stole that dies goes to its owner's graveyard, so it does NOT trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Soulcatchers' Aerie")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Test Sparrow") // owned by the opponent
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerieId = game.findPermanent("Soulcatchers' Aerie")!!
                val huskId = game.findPermanent("Nantuko Husk")!!
                val opponentBird = game.findPermanent("Test Sparrow")!!

                // Player1 steals the opponent's Bird with Threaten.
                game.castSpell(1, "Threaten", opponentBird)
                game.resolveStack()
                withClue("Player1 should now control the opponent's Bird") {
                    stateProjector.project(game.state).getController(opponentBird) shouldBe game.player1Id
                }

                // Player1 sacrifices the stolen Bird; it goes to the OPPONENT's graveyard (its owner).
                val huskAbility = cardRegistry.getCard("Nantuko Husk")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = huskId,
                        abilityId = huskAbility.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(opponentBird))
                    )
                )
                game.resolveStack()

                withClue("Bird went to its owner's (opponent's) graveyard, not yours — no feather counter") {
                    game.isInGraveyard(2, "Test Sparrow") shouldBe true
                    featherCount(game, aerieId) shouldBe 0
                }
            }

            test("a Bird you own but an opponent controls still triggers when it dies (your graveyard)") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Soulcatchers' Aerie")
                    .withCardOnBattlefield(1, "Test Sparrow") // owned by Player1
                    .withCardOnBattlefield(2, "Nantuko Husk")
                    .withCardInHand(2, "Threaten")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerieId = game.findPermanent("Soulcatchers' Aerie")!!
                val huskId = game.findPermanent("Nantuko Husk")!!
                val yourBird = game.findPermanent("Test Sparrow")!!

                // Opponent steals your Bird with Threaten.
                game.castSpell(2, "Threaten", yourBird)
                game.resolveStack()
                withClue("Opponent should now control your Bird") {
                    stateProjector.project(game.state).getController(yourBird) shouldBe game.player2Id
                }

                // Opponent sacrifices it; it goes to YOUR graveyard (you own it), so the Aerie triggers.
                val huskAbility = cardRegistry.getCard("Nantuko Husk")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = huskId,
                        abilityId = huskAbility.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(yourBird))
                    )
                )
                game.resolveStack()

                withClue("Bird went to your graveyard (you own it) — the Aerie gains a feather counter") {
                    game.isInGraveyard(1, "Test Sparrow") shouldBe true
                    featherCount(game, aerieId) shouldBe 1
                }
            }
        }
    }

    private fun featherCount(game: TestGame, aerieId: EntityId): Int =
        game.state.getEntity(aerieId)?.get<CountersComponent>()?.getCount(CounterType.FEATHER) ?: 0
}
