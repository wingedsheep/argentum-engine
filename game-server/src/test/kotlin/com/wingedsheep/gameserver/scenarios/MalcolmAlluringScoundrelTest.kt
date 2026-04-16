package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Malcolm, Alluring Scoundrel.
 *
 * Card reference:
 * - Malcolm, Alluring Scoundrel ({1}{U}) — Legendary Creature — Siren Pirate, 2/1
 *   Flash
 *   Flying
 *   "Whenever Malcolm, Alluring Scoundrel deals combat damage to a player, put a chorus
 *    counter on it. Draw a card, then discard a card. If there are four or more chorus
 *    counters on Malcolm, Alluring Scoundrel, you may cast the discarded card without
 *    paying its mana cost."
 */
class MalcolmAlluringScoundrelTest : ScenarioTestBase() {

    init {
        context("Malcolm, Alluring Scoundrel") {

            test("adds a chorus counter and prompts for discard when dealing combat damage") {
                val game = scenario()
                    .withPlayers("Malcolm Player", "Opponent")
                    .withCardOnBattlefield(1, "Malcolm, Alluring Scoundrel")
                    .withCardInHand(1, "Island")               // something to discard
                    .withCardInLibrary(1, "Forest")             // drawn during loot
                    .withCardInLibrary(1, "Mountain")           // spare to prevent deck-out
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Malcolm, Alluring Scoundrel" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance until the discard decision appears
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have a pending SelectCards decision for discard") {
                    game.hasPendingDecision() shouldBe true
                }

                // By this point the draw has happened (hand +1), about to discard (-1).
                withClue("Hand size should have grown by 1 from the loot draw before the discard") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }

                val malcolm = game.findPermanent("Malcolm, Alluring Scoundrel")!!
                val chorusCount = game.state.getEntity(malcolm)
                    ?.get<CountersComponent>()?.getCount(CounterType.CHORUS) ?: 0
                withClue("Malcolm should have 1 chorus counter") {
                    chorusCount shouldBe 1
                }

                // Discard the Island
                val islandId = game.findCardsInHand(1, "Island").first()
                game.selectCards(listOf(islandId))
                game.resolveStack()

                withClue("Island should be in graveyard after discarding") {
                    game.isInGraveyard(1, "Island") shouldBe true
                }

                // Counter count (1) is below 4, so the discarded Island should stay in the graveyard
                // and should NOT have been granted free-cast permission.
                val discardedInGraveyard = game.findCardsInGraveyard(1, "Island").firstOrNull()
                withClue("Discarded card should be in graveyard when counters < 4") {
                    discardedInGraveyard shouldBe islandId
                }

                val mayPlay = game.state.getEntity(islandId)?.get<MayPlayFromExileComponent>()
                withClue("No free-cast permission should be granted when counters < 4") {
                    mayPlay shouldBe null
                }
            }

            test("grants free-cast permission when the fourth chorus counter lands") {
                val game = scenario()
                    .withPlayers("Malcolm Player", "Opponent")
                    .withCardOnBattlefield(1, "Malcolm, Alluring Scoundrel")
                    .withCardInHand(1, "Shock")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Seed Malcolm with 3 chorus counters so combat damage pushes him to 4.
                val malcolm = game.findPermanent("Malcolm, Alluring Scoundrel")!!
                val seeded = CountersComponent().withAdded(CounterType.CHORUS, 3)
                game.state = game.state.updateEntity(malcolm) { c -> c.with(seeded) }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Malcolm, Alluring Scoundrel" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance until the discard decision appears
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have a pending SelectCards decision for discard") {
                    game.hasPendingDecision() shouldBe true
                }

                val chorusCountNow = game.state.getEntity(malcolm)
                    ?.get<CountersComponent>()?.getCount(CounterType.CHORUS) ?: 0
                withClue("Malcolm should have 4 chorus counters after the combat damage trigger") {
                    chorusCountNow shouldBe 4
                }

                val shockId = game.findCardsInHand(1, "Shock").first()
                game.selectCards(listOf(shockId))
                game.resolveStack()

                // Per oracle, Shock stays in the graveyard; the free-cast grant is applied
                // directly to the graveyard card (zone resolver accepts cards in either
                // exile or graveyard with MayPlayFromExileComponent).
                val shockContainer = game.state.getEntity(shockId)
                val inExile = game.state.getExile(game.player1Id).contains(shockId)
                withClue("Discarded Shock should NOT be moved to exile") {
                    inExile shouldBe false
                }

                val shockStillInGraveyard = game.state.getGraveyard(game.player1Id).any {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Shock"
                }
                withClue("Discarded Shock should remain in the graveyard") {
                    shockStillInGraveyard shouldBe true
                }

                val mayPlay = shockContainer?.get<MayPlayFromExileComponent>()
                withClue("Shock should have MayPlayFromExileComponent for player 1") {
                    (mayPlay != null && mayPlay.controllerId == game.player1Id) shouldBe true
                }

                val freeCast = shockContainer?.get<PlayWithoutPayingCostComponent>()
                withClue("Shock should have PlayWithoutPayingCostComponent for player 1") {
                    (freeCast != null && freeCast.controllerId == game.player1Id) shouldBe true
                }
            }
        }
    }
}
