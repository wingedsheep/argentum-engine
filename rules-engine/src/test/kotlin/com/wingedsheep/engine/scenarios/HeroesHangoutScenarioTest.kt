package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Heroes' Hangout (SPM #79) — {R} Sorcery, uncommon.
 *
 *   Choose one —
 *   • Date Night — Exile the top two cards of your library. Choose one of them. Until the end
 *     of your next turn, you may play that card.
 *   • Patrol Night — One or two target creatures each get +1/+0 and gain first strike until end
 *     of turn.
 *
 * Proven end-to-end:
 *  1. Date Night exiles BOTH top cards, but the may-play permission is granted only for the one
 *     the player chooses — and that chosen card can actually be cast from exile, while the other
 *     exiled card cannot.
 *  2. Patrol Night pumps one or two target creatures by +1/+0 and grants each first strike.
 */
class HeroesHangoutScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Heroes' Hangout — choose one") {

            test("Date Night: exiles the top two cards, only the chosen one becomes playable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heroes' Hangout")
                    .withLandsOnBattlefield(1, "Mountain", 1) // {R} for Heroes' Hangout
                    .withLandsOnBattlefield(1, "Forest", 2)   // {1}{G} to replay the chosen card
                    // Top two of library (first added = top): Grizzly Bears, then Lightning Bolt.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findCardsInLibrary(1, "Grizzly Bears").single()
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()

                game.castSpellWithMode(1, "Heroes' Hangout", modeIndex = 0).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // Reveal-and-choose one of the two top cards.
                val select = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision to choose one exiled card; got ${game.getPendingDecision()}")
                withClue("choose exactly one of the two exiled cards") {
                    select.minSelections shouldBe 1
                    select.maxSelections shouldBe 1
                }
                game.selectCards(listOf(bear))
                game.resolveStack()

                val exile = game.state.getExile(game.player1Id)
                withClue("both top cards are exiled") {
                    exile shouldContain bear
                    exile shouldContain bolt
                }
                withClue("only the chosen card carries a may-play permission") {
                    val permittedCards = game.state.mayPlayPermissions.flatMap { it.cardIds }
                    permittedCards shouldContain bear
                    permittedCards shouldNotContain bolt
                }

                // "you may play that card" — cast the chosen card from exile.
                val cast = game.castSpellFromExile(1, "Grizzly Bears")
                withClue("the chosen card can be played from exile: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears resolved onto the battlefield from exile") {
                    (game.findPermanent("Grizzly Bears") != null) shouldBe true
                }
            }

            test("Patrol Night: one or two target creatures each get +1/+0 and first strike") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heroes' Hangout")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val bearsPowerBefore = projector.getProjectedPower(game.state, bears)
                val giantPowerBefore = projector.getProjectedPower(game.state, giant)
                val bearsToughnessBefore = projector.getProjectedToughness(game.state, bears)
                val giantToughnessBefore = projector.getProjectedToughness(game.state, giant)

                val handCard = game.state.getHand(game.player1Id).single { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Heroes' Hangout"
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = handCard,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)),
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                        ),
                    )
                )
                withClue("Patrol Night casts with two targets: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("each target gets +1/+0") {
                    projector.getProjectedPower(game.state, bears) shouldBe bearsPowerBefore + 1
                    projector.getProjectedPower(game.state, giant) shouldBe giantPowerBefore + 1
                    projector.getProjectedToughness(game.state, bears) shouldBe bearsToughnessBefore
                    projector.getProjectedToughness(game.state, giant) shouldBe giantToughnessBefore
                }
                withClue("each target gains first strike") {
                    projector.getProjectedKeywords(game.state, bears) shouldContain Keyword.FIRST_STRIKE
                    projector.getProjectedKeywords(game.state, giant) shouldContain Keyword.FIRST_STRIKE
                }
            }

            test("Patrol Night: a single target creature gets +1/+0 and first strike") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heroes' Hangout")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val powerBefore = projector.getProjectedPower(game.state, bears)

                game.castSpellWithMode(1, "Heroes' Hangout", modeIndex = 1, targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the single target gets +1/+0 and first strike") {
                    projector.getProjectedPower(game.state, bears) shouldBe powerBefore + 1
                    projector.getProjectedKeywords(game.state, bears) shouldContain Keyword.FIRST_STRIKE
                }
            }
        }
    }
}
