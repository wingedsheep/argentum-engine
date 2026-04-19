package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Shadow Urchin.
 *
 * Shadow Urchin {2}{B/R} — 3/4 Creature — Ouphe
 * Whenever this creature attacks, blight 1.
 * Whenever a creature you control with one or more counters on it dies, exile that many
 * cards from the top of your library. Until your next end step, you may play those cards.
 */
class ShadowUrchinScenarioTest : ScenarioTestBase() {

    private val oneOne = CardDefinition.creature(
        name = "Fragile Warrior",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Warrior")),
        power = 1, toughness = 1
    )

    init {
        cardRegistry.register(oneOne)

        context("Shadow Urchin") {

            test("attack trigger blights a creature you control with a -1/-1 counter") {
                val game = scenario()
                    .withPlayers("Shadow", "Opponent")
                    .withCardOnBattlefield(1, "Shadow Urchin", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Shadow Urchin" to 2))
                withClue("Attack declaration should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Attack trigger goes on the stack and pauses for the blight target.
                game.resolveStack()

                withClue("Blight 1 should prompt for a -1/-1 target (pick the Bears)") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(bears))

                // Resolve anything still on the stack (the trigger itself resolves upon selection).
                game.resolveStack()

                val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                withClue("Grizzly Bears should now have one -1/-1 counter from blight 1") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
                }
            }

            test("dies-with-counters trigger exiles that many cards with may-play permission") {
                val game = scenario()
                    .withPlayers("Shadow", "Opponent")
                    .withCardOnBattlefield(1, "Shadow Urchin", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                // Put 2 -1/-1 counters on Grizzly Bears (now 0/0). SBAs haven't fired yet
                // because the state was hand-built; the next action (Shock resolution) will
                // trigger the SBA check that kills the creature.
                val counters = CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 2)
                game.state = game.state.updateEntity(bears) { c -> c.with(counters) }

                // Casting Shock targeting Bears isn't strictly needed — any action that causes
                // priority-pass-with-SBA-check will kill it. Use Shock on the Bears for a full
                // end-to-end: even though Bears already has 0 toughness, resolving Shock runs
                // SBAs, puts it in the graveyard with its 2 counters, and fires Urchin's trigger.
                game.castSpell(1, "Shock", bears)
                game.resolveStack()

                withClue("Grizzly Bears should have died from -1/-1 counters during SBA check") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                val exile = game.state.getExile(game.player1Id)
                withClue("Expected exactly two cards in exile (equal to counter count on the dying creature)") {
                    exile.size shouldBe 2
                }

                for (cardId in exile) {
                    val mayPlay = game.state.getEntity(cardId)?.get<MayPlayFromExileComponent>()
                    withClue("Exiled card ${game.state.getEntity(cardId)?.get<CardComponent>()?.name} should have MayPlayFromExileComponent") {
                        mayPlay.shouldNotBeNull()
                        mayPlay.controllerId shouldBe game.player1Id
                    }
                }

                // Library started with 2; exiled 2 → should be empty.
                withClue("P1's library should be empty (started with 2, exiled 2)") {
                    game.librarySize(1) shouldBe 0
                }
            }

            test("dies trigger does not fire when the dying creature has no counters") {
                val game = scenario()
                    .withPlayers("Shadow", "Opponent")
                    .withCardOnBattlefield(1, "Shadow Urchin", summoningSickness = false)
                    .withCardOnBattlefield(1, "Fragile Warrior", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warrior = game.findPermanent("Fragile Warrior")!!
                // Move directly to graveyard with no counters.
                game.state = game.state
                    .removeFromZone(com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.BATTLEFIELD), warrior)
                    .addToZone(com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD), warrior)

                game.passPriority()
                game.resolveStack()

                withClue("No cards should be exiled — the dying creature had zero counters") {
                    game.state.getExile(game.player1Id).size shouldBe 0
                }
            }
        }
    }
}
