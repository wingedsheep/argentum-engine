package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Bioengineered Future (EOE #172) — {1}{G}{G} Enchantment.
 *
 *   "When this enchantment enters, create a Lander token.
 *    Each creature you control enters with an additional +1/+1 counter on it for each land
 *    that entered the battlefield under your control this turn."
 *
 * Covers both the new per-player land-ETB tracker
 * ([PermanentsEnteredUnderControlThisTurnComponent], TurnTracker.LANDS_ENTERED_UNDER_CONTROL) and
 * the third-party ETB replacement that scales with it. The Lander-token half is shared with
 * Biotech Specialist / Kav Landseeker / etc. and tested in those.
 */
class BioengineeredFutureScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, entityId: EntityId): Int =
        game.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun landsEntered(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)
            ?.get<PermanentsEnteredUnderControlThisTurnComponent>()
            ?.countOfType(com.wingedsheep.sdk.core.CardType.LAND) ?: 0

    private fun forestInHand(game: TestGame, playerId: EntityId): EntityId =
        game.state.getHand(playerId).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
        }

    // LANDS_PLAYED (the older tracker the new one diverges from) counts only from-hand land
    // drops; it equals maxPerTurn minus the remaining drops on LandDropsComponent.
    private fun landDropsUsed(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<LandDropsComponent>()
            ?.let { it.maxPerTurn - it.remaining } ?: 0

    init {
        // A trivially-cheap creature so the test can cast multiple in a turn without
        // needing extra mana setup or worrying about color identity.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Beast",
                manaCost = ManaCost.parse("{G}"),
                subtypes = setOf(Subtype.BEAST),
                power = 1,
                toughness = 1
            )
        )

        context("Bioengineered Future") {

            test("a creature entering after one land this turn gets one extra +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bioengineered Future")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 5) // mana for Test Beast
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(PlayLand(game.player1Id, forestInHand(game, game.player1Id)))
                    .error shouldBe null
                landsEntered(game, game.player1Id) shouldBe 1

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                val beast = game.findPermanent("Test Beast")!!
                withClue("creature enters with +1/+1 counter for the one land that entered this turn") {
                    plusOneCounters(game, beast) shouldBe 1
                }
            }

            test("a land that enters without a land drop still counts (diverges from LANDS_PLAYED)") {
                // The whole reason this tracker exists: a Rampant Growth / Lander-style land
                // ETB is NOT a land drop, so LANDS_PLAYED stays 0 while
                // LANDS_ENTERED_UNDER_CONTROL must bump. This exercises the
                // ZoneTransitionService entry path (search -> battlefield), not the PlayLand
                // path the other tests cover.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bioengineered Future")
                    .withCardInHand(1, "Rampant Growth")
                    .withCardInHand(1, "Test Beast")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 5) // mana for Rampant Growth + Beast
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player = game.player1Id
                game.castSpell(1, "Rampant Growth").error shouldBe null
                game.resolveStack()

                // Fetch the basic land onto the battlefield (no land drop consumed).
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()

                withClue("the searched land entered without being played as a land drop") {
                    landDropsUsed(game, player) shouldBe 0
                }
                withClue("yet the land-entered tracker counts it") {
                    landsEntered(game, player) shouldBe 1
                }

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                val beast = game.findPermanent("Test Beast")!!
                withClue("creature gets the +1/+1 counter from the non-drop land ETB") {
                    plusOneCounters(game, beast) shouldBe 1
                }
            }

            test("the count scales with multiple lands that entered this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bioengineered Future")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Three land drops via Summer Bloom-style additional plays would normally be
                // needed for the second/third drop, but we sidestep that by inflating the
                // remaining land-drop count directly — the tracker reads ETBs, not drops.
                val player = game.player1Id
                game.state = game.state.updateEntity(player) { container ->
                    val drops = container
                        .get<com.wingedsheep.engine.state.components.player.LandDropsComponent>()!!
                    container.with(drops.copy(remaining = 3))
                }

                repeat(3) {
                    game.execute(PlayLand(player, forestInHand(game, player))).error shouldBe null
                }
                landsEntered(game, player) shouldBe 3

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                val beast = game.findPermanent("Test Beast")!!
                withClue("three lands ETB'd this turn -> +3/+3 counter bonus") {
                    plusOneCounters(game, beast) shouldBe 3
                }
            }

            test("lands that entered before Bioengineered Future still count") {
                // Per Scryfall ruling 2025-07-25:
                //   "Bioengineered Future's last ability will consider lands that entered the
                //    battlefield under your control before it was on the battlefield."
                // The tracker accumulates from the start of the turn, so a Forest ETB before
                // BF is cast still bumps the count BF reads when a creature ETBs later.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bioengineered Future")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player = game.player1Id

                // Forest first — tracker becomes 1.
                game.execute(PlayLand(player, forestInHand(game, player))).error shouldBe null
                landsEntered(game, player) shouldBe 1

                // Then Bioengineered Future. BF entering shouldn't disturb the land tracker
                // (it's an Enchantment, not a land).
                game.castSpell(1, "Bioengineered Future").error shouldBe null
                game.resolveStack()
                landsEntered(game, player) shouldBe 1

                // Then a creature — even though the land entered before BF, BF still sees it.
                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                val beast = game.findPermanent("Test Beast")!!
                withClue("the pre-BF Forest still contributes one +1/+1 counter") {
                    plusOneCounters(game, beast) shouldBe 1
                }
            }

            test("the tracker is per-player — opponent's land ETB doesn't feed our count") {
                // Bioengineered Future reads its own controller's count via
                // DynamicAmount.TurnTracking(Player.You, ...). The tracker is a player-side
                // component, so an opponent's land ETB lands in the opponent's counter, not
                // ours. Verifying the tracker directly is the cleanest signal here; the
                // consumer side ("creature gets +1/+1") is covered by the earlier tests.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponent = game.player2Id
                game.execute(PlayLand(opponent, forestInHand(game, opponent))).error shouldBe null

                landsEntered(game, opponent) shouldBe 1
                landsEntered(game, game.player1Id) shouldBe 0
            }

            test("the tracker is cleared at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bioengineered Future")
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player = game.player1Id
                game.execute(PlayLand(player, forestInHand(game, player))).error shouldBe null
                landsEntered(game, player) shouldBe 1

                // Pass to the opponent's draw step — that crosses the cleanup boundary that
                // strips this-turn trackers.
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                landsEntered(game, player) shouldBe 0
            }
        }
    }
}
