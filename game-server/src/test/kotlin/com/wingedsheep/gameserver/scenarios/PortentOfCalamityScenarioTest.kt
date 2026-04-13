package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Portent of Calamity.
 *
 * Card reference:
 * - Portent of Calamity {X}{U} — Sorcery
 *   "Reveal the top X cards of your library. For each card type, you may exile a card
 *   of that type from among them. Put the rest into your graveyard. You may cast a spell
 *   from among the exiled cards without paying its mana cost if you exiled four or more
 *   cards this way. Then put the rest of the exiled cards into your hand."
 *
 * The card relies on three pieces of pipeline infrastructure:
 *  1. SelectionRestriction.OnePerCardType — caps the selection's max at the number of
 *     distinct card types present and normalizes the player's response server-side.
 *  2. ConditionalOnCollectionEffect.minSize — gates the free-cast branch on "4 or more".
 *  3. GrantFreeCastTargetFromExileEffect with EffectTarget.PipelineTarget — grants the
 *     chosen exiled card a free-cast permission.
 *
 * Each test uses a pile of cards with distinct card types (creature / instant / sorcery /
 * artifact / enchantment) so the per-card-type cap matches the submitted selection.
 */
class PortentOfCalamityScenarioTest : ScenarioTestBase() {

    init {
        context("Portent of Calamity") {

            test("with X=3 and 3 distinct types, exiles all three and puts them in hand (no free cast)") {
                // Library top 3: Grizzly Bears (creature), Shivan Fire (instant), Stone Rain (sorcery).
                // 3 distinct types → all three exiled → 3 < 4 → all move to hand.
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Portent of Calamity")
                    .withLandsOnBattlefield(1, "Island", 4) // {X=3}{U}
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Shivan Fire")
                    .withCardInLibrary(1, "Stone Rain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.state.getLibrary(game.player1Id).toList()
                val bears = libraryBefore.single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
                val shivan = libraryBefore.single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Shivan Fire" }
                val stoneRain = libraryBefore.single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Stone Rain" }

                game.castXSpell(1, "Portent of Calamity", xValue = 3)
                game.resolveStack()

                // Pipeline pauses for the exile-selection decision.
                withClue("Expected SelectCardsDecision for one-per-card-type exile choice") {
                    game.hasPendingDecision() shouldBe true
                }

                // Player exiles one of each type (3 distinct types → 3 cards).
                game.selectCards(listOf(bears, shivan, stoneRain))
                game.resolveStack()

                // 3 exiled, which is less than 4 → the conditional takes the ifEmpty branch
                // (MoveCollection exiled → hand). No further decision.
                withClue("No decision should be pending. Got: ${game.getPendingDecision()}") {
                    game.hasPendingDecision() shouldBe false
                }

                // All 3 cards are now in hand. Graveyard holds only Portent itself.
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Shivan Fire") shouldBe true
                game.isInHand(1, "Stone Rain") shouldBe true
                game.isInGraveyard(1, "Portent of Calamity") shouldBe true
                game.state.getExile(game.player1Id) shouldHaveSize 0
            }

            test("with X=5 and 5 distinct types, 4+ exiled unlocks a free cast from exile") {
                // Library top 5: Grizzly Bears (creature), Shivan Fire (instant),
                //                Stone Rain (sorcery), Icy Manipulator (artifact),
                //                Fungal Plots (enchantment). All 5 distinct card types.
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Portent of Calamity")
                    .withLandsOnBattlefield(1, "Island", 6) // {X=5}{U}
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Shivan Fire")
                    .withCardInLibrary(1, "Stone Rain")
                    .withCardInLibrary(1, "Icy Manipulator")
                    .withCardInLibrary(1, "Fungal Plots")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Portent of Calamity", xValue = 5)
                game.resolveStack()

                // First decision: choose cards to exile (one per type → all 5).
                game.hasPendingDecision() shouldBe true
                val top5 = game.state.getLibrary(game.player1Id).take(5).toList()
                game.selectCards(top5)
                game.resolveStack()

                // Second decision: free-cast selection — "choose up to 1" from the 5 exiled cards.
                withClue("Expected the free-cast ChooseUpTo(1) decision after 4+ cards were exiled") {
                    game.hasPendingDecision() shouldBe true
                }

                // Pick Shivan Fire in exile for the free cast.
                val exileBefore = game.state.getExile(game.player1Id)
                val shivanInExile = exileBefore.single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Shivan Fire"
                }
                game.selectCards(listOf(shivanInExile))
                game.resolveStack()

                // Shivan Fire remains in exile with free-cast permission.
                val shivanContainer = game.state.getEntity(shivanInExile)!!
                shivanContainer.get<MayPlayFromExileComponent>() shouldBe
                    MayPlayFromExileComponent(controllerId = game.player1Id)
                shivanContainer.get<PlayWithoutPayingCostComponent>() shouldBe
                    PlayWithoutPayingCostComponent(controllerId = game.player1Id)
                game.state.getExile(game.player1Id).contains(shivanInExile) shouldBe true

                // The other 4 exiled cards went to hand.
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Stone Rain") shouldBe true
                game.isInHand(1, "Icy Manipulator") shouldBe true
                game.isInHand(1, "Fungal Plots") shouldBe true
                game.isInHand(1, "Shivan Fire") shouldBe false
            }

            test("declining the free cast sends all exiled cards to hand") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Portent of Calamity")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Shivan Fire")
                    .withCardInLibrary(1, "Stone Rain")
                    .withCardInLibrary(1, "Icy Manipulator")
                    .withCardInLibrary(1, "Fungal Plots")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Portent of Calamity", xValue = 5)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.selectCards(game.state.getLibrary(game.player1Id).take(5).toList())
                game.resolveStack()

                // Decline the free cast.
                game.hasPendingDecision() shouldBe true
                game.skipSelection()
                game.resolveStack()

                // All 5 exiled cards are now in hand; exile is empty.
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Shivan Fire") shouldBe true
                game.isInHand(1, "Stone Rain") shouldBe true
                game.isInHand(1, "Icy Manipulator") shouldBe true
                game.isInHand(1, "Fungal Plots") shouldBe true
                game.state.getExile(game.player1Id) shouldHaveSize 0
            }

            test("artifact creature counts as two types - 3 cards can produce 4 distinct types triggering free cast") {
                // Library top 3: Barkform Harvester (Artifact Creature), Shivan Fire (Instant), Island (Land)
                // Types covered: Artifact + Creature + Instant + Land = 4 distinct types
                // 4 ≥ 4 → free-cast branch triggers even though only 3 cards were exiled.
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Portent of Calamity")
                    .withLandsOnBattlefield(1, "Island", 4) // {X=3}{U}
                    .withCardInLibrary(1, "Barkform Harvester")  // Artifact Creature
                    .withCardInLibrary(1, "Shivan Fire")         // Instant
                    .withCardInLibrary(1, "Island")              // Basic Land
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Portent of Calamity", xValue = 3)
                game.resolveStack()

                // Exile all 3 cards (each has a unique card type relative to the others).
                game.hasPendingDecision() shouldBe true
                val options = (game.getPendingDecision() as com.wingedsheep.engine.core.SelectCardsDecision).options
                game.selectCards(options.toList())
                game.resolveStack()

                // 3 cards exiled covering 4 distinct types (Artifact, Creature, Instant, Land)
                // → free-cast selection should appear.
                withClue("Free-cast decision should appear because artifact creature counts as 2 types (4 total ≥ 4)") {
                    game.hasPendingDecision() shouldBe true
                }

                // Decline the free cast — all 3 should go to hand.
                game.skipSelection()
                game.resolveStack()

                game.isInHand(1, "Barkform Harvester") shouldBe true
                game.isInHand(1, "Shivan Fire") shouldBe true
                game.isInHand(1, "Island") shouldBe true
                game.state.getExile(game.player1Id) shouldHaveSize 0
            }

            test("pile of three creatures limits exile selection to a single card") {
                // When every revealed card shares the creature type, OnePerCardType caps
                // the selection max at 1 — the player can only exile one of them.
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Portent of Calamity")
                    .withLandsOnBattlefield(1, "Island", 4) // {X=3}{U}
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Bog Imp")
                    .withCardInLibrary(1, "Anaconda")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Portent of Calamity", xValue = 3)
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("Expected a SelectCardsDecision") {
                    (decision is com.wingedsheep.engine.core.SelectCardsDecision) shouldBe true
                }
                decision as com.wingedsheep.engine.core.SelectCardsDecision
                withClue("OnePerCardType should cap maxSelections at 1 for a pile of three creatures") {
                    decision.maxSelections shouldBe 1
                }

                // Exile the first creature; the other two go to the graveyard.
                val bears = decision.options.single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                game.selectCards(listOf(bears))
                game.resolveStack()

                game.hasPendingDecision() shouldBe false

                // One exiled creature moves to hand (< 4 exiled → no free cast).
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Bog Imp") shouldBe true
                game.isInGraveyard(1, "Anaconda") shouldBe true
            }
        }
    }
}
