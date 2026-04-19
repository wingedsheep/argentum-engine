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
                    .withCardOnBattlefield(1, "Fragile Warrior", summoningSickness = false) // 1/1
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warrior = game.findPermanent("Fragile Warrior")!!

                // Pre-place 2 -1/-1 counters on the 1/1 so it's a doomed -1/-1.
                val counters = CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 2)
                game.state = game.state.updateEntity(warrior) { c -> c.with(counters) }

                // Passing priority triggers SBAs: the 0-toughness warrior dies, and
                // Shadow Urchin's dies-with-counters trigger fires for 2 cards.
                game.passPriority()
                game.resolveStack()

                withClue("Fragile Warrior should have died to state-based actions") {
                    game.isOnBattlefield("Fragile Warrior") shouldBe false
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

                // Only 1 card left in P1's library (started with 3, exiled 2).
                withClue("P1's library should have one card left") {
                    game.librarySize(1) shouldBe 1
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
