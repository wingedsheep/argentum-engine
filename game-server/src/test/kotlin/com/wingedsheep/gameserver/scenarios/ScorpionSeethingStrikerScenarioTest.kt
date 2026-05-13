package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Rarity
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Scorpion, Seething Striker.
 *
 * Card reference:
 * - Scorpion, Seething Striker ({3}{B}): Legendary Creature — Scorpion Human Villain, 3/3
 *   "Deathtouch"
 *   "At the beginning of your end step, if a creature died this turn, target creature
 *    you control connives."
 *   Uncommon, collector number 64, artist Simon Dominic
 */
class ScorpionSeethingStrikerScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Scorpion, Seething Striker — card definition") {

            test("cast with {3}{B} enters battlefield as a 3/3 Legendary Creature — Scorpion Human Villain with deathtouch") {
                // 1 Swamp supplies {B}, 3 Islands supply the 3 generic mana.
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardInHand(1, "Scorpion, Seething Striker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Scorpion, Seething Striker")
                withClue("Casting Scorpion, Seething Striker for {3}{B} should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Scorpion, Seething Striker should be on the battlefield") {
                    game.isOnBattlefield("Scorpion, Seething Striker") shouldBe true
                }

                val scorpionId = game.findPermanent("Scorpion, Seething Striker")!!
                val projected = stateProjector.project(game.state)

                withClue("Scorpion should be a 3/3") {
                    projected.getPower(scorpionId) shouldBe 3
                    projected.getToughness(scorpionId) shouldBe 3
                }

                withClue("Scorpion should have deathtouch") {
                    projected.hasKeyword(scorpionId, Keyword.DEATHTOUCH) shouldBe true
                }

                val clientState = game.getClientState(1)
                val card = clientState.cards[scorpionId]
                card.shouldNotBeNull()

                withClue("Scorpion should be a Legendary Creature") {
                    card.cardTypes shouldContain "CREATURE"
                    card.typeLine.contains("Legendary") shouldBe true
                }

                withClue("Scorpion should have correct subtypes") {
                    card.subtypes shouldContain "Scorpion"
                    card.subtypes shouldContain "Human"
                    card.subtypes shouldContain "Villain"
                }
            }
        }

        context("Scorpion, Seething Striker — end-step connive trigger") {

            test("end-step trigger fires if a creature died this turn and connive grants +1/+1 counter on nonland discard") {
                // Glory Seeker (2/2) is the connive target.
                // Grizzly Bears in hand will be discarded as a nonland, granting a +1/+1 counter.
                // Forest on top of library will be drawn by connive.
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Scorpion, Seething Striker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Simulate a creature having died earlier this turn (e.g., killed by a spell).
                game.state = game.state.updateEntity(game.player1Id) { container ->
                    container.with(CreaturesDiedThisTurnComponent(count = 1))
                }

                val glorySeekerIdBefore = game.findPermanent("Glory Seeker")!!
                val initialHandSize = game.handSize(1)

                // Advance to the end step — the intervening-if condition is true so the
                // triggered ability should fire and pause for target selection.
                game.passUntilPhase(Phase.ENDING, Step.END)

                withClue("triggered ability should be awaiting target selection at end step") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker as the connive target.
                game.selectTargets(listOf(glorySeekerIdBefore))

                // Ability resolves: draws one card then pauses for discard.
                game.resolveStack()

                withClue("connive draw: hand should have grown by one before discard") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }
                withClue("connive discard: should be awaiting discard selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Discard Grizzly Bears (nonland) — Glory Seeker should gain a +1/+1 counter.
                val nonlandToDiscard = game.findCardsInHand(1, "Grizzly Bears").first()
                game.selectCards(listOf(nonlandToDiscard))
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 3/3 after +1/+1 counter from nonland discard") {
                    projected.getPower(glorySeekerIdBefore) shouldBe 3
                    projected.getToughness(glorySeekerIdBefore) shouldBe 3
                }
            }
        }

        context("Scorpion, Seething Striker — intervening-if suppresses trigger when no creature died") {

            test("end-step trigger does not fire when no creature died this turn and no connive occurs") {
                // No creature has died this turn — CreaturesDiedThisTurnComponent is absent.
                // Glory Seeker stays 2/2; no card is drawn or discarded from the ability.
                val game = scenario()
                    .withPlayers("Active", "Opponent")
                    .withCardOnBattlefield(1, "Scorpion, Seething Striker")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerIdBefore = game.findPermanent("Glory Seeker")!!
                val initialHandSize = game.handSize(1)
                val initialLibrarySize = game.librarySize(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("triggered ability must not fire — no creature died this turn") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no card should be drawn from suppressed trigger") {
                    game.handSize(1) shouldBe initialHandSize
                }
                withClue("no card should be drawn from library") {
                    game.librarySize(1) shouldBe initialLibrarySize
                }
                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should still be 2/2 — no connive counter placed") {
                    projected.getPower(glorySeekerIdBefore) shouldBe 2
                    projected.getToughness(glorySeekerIdBefore) shouldBe 2
                }
            }
        }

        context("Scorpion, Seething Striker — metadata matches Scryfall") {

            test("card definition has correct rarity, collector number, artist, oracle text, and image URI") {
                val cardDef = cardRegistry.getCard("Scorpion, Seething Striker")
                cardDef.shouldNotBeNull()

                withClue("rarity should be UNCOMMON") {
                    cardDef.metadata.rarity shouldBe Rarity.UNCOMMON
                }
                withClue("collector number should be 64") {
                    cardDef.metadata.collectorNumber shouldBe "64"
                }
                withClue("artist should be Simon Dominic") {
                    cardDef.metadata.artist shouldBe "Simon Dominic"
                }
                withClue("oracle text should list Deathtouch") {
                    cardDef.oracleText.contains("Deathtouch") shouldBe true
                }
                withClue("image URI should be from Scryfall") {
                    cardDef.metadata.imageUri.shouldNotBeNull()
                    cardDef.metadata.imageUri!!.startsWith("https://cards.scryfall.io/") shouldBe true
                }
            }
        }
    }
}
