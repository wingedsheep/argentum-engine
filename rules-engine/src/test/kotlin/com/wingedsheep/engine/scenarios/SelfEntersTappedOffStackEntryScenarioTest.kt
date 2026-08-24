package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A permanent's own printed "[this permanent] enters tapped" clause is a replacement effect the
 * object carries about itself (CR 614.1d), applied as it enters (CR 614.12 — "Such effects may come
 * from the permanent itself if they affect only that permanent"). Nothing in that rule cares *how*
 * the permanent got to the battlefield, so a reanimated, blinked or fetched tapland must arrive
 * tapped exactly like one that was played from hand.
 *
 * The engine reads the clause on three disjoint paths, and until this change only two of them
 * existed:
 *  - **playing a land** — `PlayLandHandler`, which never calls `ZoneTransitionService.moveToZone`;
 *  - **resolving a permanent spell** — `StackResolver`, which places permanents with `addToZone`;
 *  - **every other card-based entry** — reanimation and returns from exile
 *    (`MoveToZoneEffectExecutor`) and search-library / collection moves (`MoveCollectionExecutor`),
 *    both of which funnel through `moveToZone`. That third path ignored the clause entirely and the
 *    permanent entered untapped.
 *
 * These tests pin the third path down in all three printed shapes, pin the CR 616.1e ordering
 * against an "enters untapped" replacement, and guard the two paths that already worked against a
 * double application.
 */
class SelfEntersTappedOffStackEntryScenarioTest : ScenarioTestBase() {

    private fun GameState.isTapped(entityId: EntityId): Boolean =
        getEntity(entityId)?.has<TappedComponent>() == true

    /** The entity id of [cardName] sitting in player 1's graveyard. */
    private fun graveyardCard(state: GameState, ownerId: EntityId, cardName: String): EntityId =
        state.getGraveyard(ownerId).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == cardName
        }

    init {

        context("a card's own 'enters tapped' applies to an off-stack entry") {

            test("a reanimation spell brings back a creature that says 'enters tapped' TAPPED") {
                // The MoveToZoneEffectExecutor path, through a real card: Zombify is
                // Effects.PutOntoBattlefieldFromGraveyard, and it says nothing about tapped.
                // Diregraf Ghoul's own line does.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Zombify")
                    .withCardInGraveyard(1, "Diregraf Ghoul")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ghoul = graveyardCard(game.state, game.player1Id, "Diregraf Ghoul")
                val cast = game.castSpellTargetingGraveyardCard(1, "Zombify", listOf(ghoul))
                withClue("casting Zombify should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the reanimated creature is on the battlefield") {
                    game.findPermanent("Diregraf Ghoul") shouldBe ghoul
                }
                withClue("CR 614.1d — its own printed clause applies however it entered") {
                    game.state.isTapped(ghoul) shouldBe true
                }
            }

            test("a tapland moved from the graveyard onto the battlefield enters TAPPED") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Jungle Hollow")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = graveyardCard(game.state, game.player1Id, "Jungle Hollow")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, land, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                moved.state.isTapped(land) shouldBe true
            }

            test("a land with no such clause still enters untapped") {
                // The control: the new reconciliation must not tap everything that arrives by
                // effect.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = graveyardCard(game.state, game.player1Id, "Forest")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, forest, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                moved.state.isTapped(forest) shouldBe false
            }

            test("a search-library move honors the fetched land's own clause") {
                // The MoveCollectionExecutor path. Wood Elves' trigger is
                // Patterns.Library.searchLibrary(destination = BATTLEFIELD) — it asks for no
                // placement at all, so Highland Forest's own "enters tapped" is the only thing
                // that can tap it.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wood Elves")
                    .withCardInLibrary(1, "Highland Forest")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Wood Elves")
                withClue("casting Wood Elves should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val search = game.state.pendingDecision as? SelectCardsDecision
                    ?: error("expected a library search; got ${game.state.pendingDecision}")
                val fetched = search.options.first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Highland Forest"
                }
                game.submitDecision(CardsSelectedResponse(search.id, listOf(fetched)))
                game.resolveStack()

                withClue("the fetched land is on the battlefield") {
                    game.findPermanent("Highland Forest") shouldBe fetched
                }
                withClue("and it entered tapped, as its own line says") {
                    game.state.isTapped(fetched) shouldBe true
                }
            }
        }

        context("the 'unless' shape is evaluated, not assumed") {

            // Overgrown Farmland: "This land enters tapped unless you control two or more other
            // lands." The permanent enters tapped when the condition is FALSE — the same polarity
            // the cast path uses.

            test("condition false — reanimated with no other lands, it enters TAPPED") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Overgrown Farmland")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = graveyardCard(game.state, game.player1Id, "Overgrown Farmland")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, land, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                moved.state.isTapped(land) shouldBe true
            }

            test("condition true — reanimated with two other lands, it enters UNTAPPED") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Overgrown Farmland")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = graveyardCard(game.state, game.player1Id, "Overgrown Farmland")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, land, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                moved.state.isTapped(land) shouldBe false
            }
        }

        context("CR 616.1e — an applicable 'enters untapped' still wins") {

            test("The Wandering Minstrel untaps a reanimated tapland") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Wandering Minstrel")
                    .withCardInGraveyard(1, "Jungle Hollow")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = graveyardCard(game.state, game.player1Id, "Jungle Hollow")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, land, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                withClue("\"Lands you control enter untapped\" beats the land's own clause") {
                    moved.state.isTapped(land) shouldBe false
                }
            }

            test("without the Minstrel the same land enters tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Jungle Hollow")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = graveyardCard(game.state, game.player1Id, "Jungle Hollow")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, land, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                moved.state.isTapped(land) shouldBe true
            }
        }

        context("the shock-land shape resolves fail-closed") {

            test("a shock land put onto the battlefield by an effect enters TAPPED, unasked") {
                // Godless Shrine is EntersTapped(payLifeCost = 2). Offering the choice needs a
                // continuation the two off-stack executors don't have yet, so moveToZone resolves
                // the pair to the outcome a player who declines gets: tapped. Deliberately
                // asserted, because the alternative (silently untapped) is the bug this change
                // fixes.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Godless Shrine")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val land = graveyardCard(game.state, game.player1Id, "Godless Shrine")
                val moved = ZoneTransitionService.moveToZone(
                    game.state, land, Zone.BATTLEFIELD,
                    ZoneEntryOptions(controllerId = game.player1Id)
                )

                moved.state.isTapped(land) shouldBe true
                withClue("a pure state transition cannot pause, so nothing was asked") {
                    moved.state.pendingDecision shouldBe null
                }
            }
        }

        context("the two paths that already read the clause are unchanged") {

            test("playing a tapland from hand still enters it tapped, exactly once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Jungle Hollow")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val inHand = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Jungle Hollow"
                }
                val played = game.execute(PlayLand(game.player1Id, inHand))
                withClue("playing the land should succeed: ${played.error}") {
                    played.error shouldBe null
                }

                val land = game.findPermanent("Jungle Hollow")
                    ?: error("the played land is not on the battlefield")
                game.state.isTapped(land) shouldBe true
            }

            test("casting a creature that says 'enters tapped' still enters it tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Diregraf Ghoul")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Diregraf Ghoul")
                withClue("casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val ghoul = game.findPermanent("Diregraf Ghoul")
                    ?: error("the cast creature is not on the battlefield")
                game.state.isTapped(ghoul) shouldBe true
            }
        }
    }
}
