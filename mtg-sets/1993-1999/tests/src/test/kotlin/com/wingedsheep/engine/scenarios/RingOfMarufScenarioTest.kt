package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ring of Ma'rûf (ARN #68) — {5} Artifact.
 *
 * "{5}, {T}, Exile this artifact: The next time you would draw a card this turn, instead put a
 *  card you own from outside the game into your hand."
 *
 * The wish that replaces a draw instead of resolving into one. Covers the shield firing on the
 * next draw (the fetched card comes from the sideboard and the draw itself is replaced, so the
 * library is untouched), the *mandatory* selection — no "may", so a lone sideboard card is taken
 * without offering a decline — and the empty-sideboard case, where the draw is still eaten.
 */
class RingOfMarufScenarioTest : ScenarioTestBase() {

    private val ringAbilityId =
        cardRegistry.getCard("Ring of Ma'rûf")!!.activatedAbilities.first().id

    // A free draw spell to provide the "you would draw a card" the shield replaces.
    private val drawOne = card("Draw One Test") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }

    init {
        cardRegistry.register(drawOne)

        context("Ring of Ma'rûf") {

            test("replaces the next draw with a card chosen from outside the game") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ring of Ma'rûf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 5)   // pay the {5}
                    .withCardInHand(1, "Draw One Test")
                    .withCardInLibrary(1, "Plains")             // the draw that gets replaced
                    .withCardInSideboard(1, "Grizzly Bears")
                    .withCardInSideboard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Ring of Ma'rûf")!!,
                        abilityId = ringAbilityId,
                    )
                )
                withClue("Activating Ring of Ma'rûf should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Exile this artifact is part of the cost — the Ring is gone on activation") {
                    game.findPermanent("Ring of Ma'rûf") shouldBe null
                    game.isInExile(1, "Ring of Ma'rûf") shouldBe true
                }

                // Trigger the replaced draw; the shield fires and asks which sideboard card to take.
                game.castSpell(1, "Draw One Test")
                game.resolveStack()

                withClue("The replaced draw should pause for the outside-the-game choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                decision.shouldNotBeNull()
                val offered = decision.cardInfo!!
                withClue("Any card you own from outside the game is offered") {
                    offered.values.any { it.name == "Grizzly Bears" } shouldBe true
                    offered.values.any { it.name == "Lightning Bolt" } shouldBe true
                }

                val boltId = offered.entries.first { it.value.name == "Lightning Bolt" }.key
                game.selectCards(listOf(boltId))
                game.resolveStack()

                withClue("The chosen card is in hand") { game.isInHand(1, "Lightning Bolt") shouldBe true }
                withClue("...and has left the sideboard") { game.isInSideboard(1, "Lightning Bolt") shouldBe false }
                withClue("The draw was replaced, so the library is untouched") {
                    game.librarySize(1) shouldBe libraryBefore
                    game.isInHand(1, "Plains") shouldBe false
                }
            }

            test("is mandatory — a lone sideboard card is taken with no chance to decline") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ring of Ma'rûf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInHand(1, "Draw One Test")
                    .withCardInLibrary(1, "Plains")
                    .withCardInSideboard(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Ring of Ma'rûf")!!,
                        abilityId = ringAbilityId,
                    )
                )
                game.resolveStack()

                game.castSpell(1, "Draw One Test")
                game.resolveStack()

                withClue("There is no 'may' — with one legal card the engine takes it outright") {
                    game.hasPendingDecision() shouldBe false
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                    game.isInSideboard(1, "Lightning Bolt") shouldBe false
                }
                withClue("The draw is still replaced") { game.isInHand(1, "Plains") shouldBe false }
            }

            test("an empty sideboard still eats the draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ring of Ma'rûf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInHand(1, "Draw One Test")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Ring of Ma'rûf")!!,
                        abilityId = ringAbilityId,
                    )
                )
                game.resolveStack()

                game.castSpell(1, "Draw One Test")
                game.resolveStack()

                withClue("Nothing to fetch, and no stall on an impossible choice") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("The draw was replaced all the same — the Plains stays in the library") {
                    game.librarySize(1) shouldBe libraryBefore
                    game.isInHand(1, "Plains") shouldBe false
                }
            }
        }
    }
}
