package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Reckless Crew (KHM #146) — {3}{R} Sorcery.
 *
 *   Create X 2/1 red Dwarf Berserker creature tokens, where X is the number of Vehicles you
 *   control plus the number of Equipment you control. For each of those tokens, you may attach an
 *   Equipment you control to it.
 */
class RecklessCrewScenarioTest : ScenarioTestBase() {

    private fun TestGame.attachedTo(id: EntityId): EntityId? =
        state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    private fun TestGame.pendingSelection(): SelectCardsDecision {
        val decision = getPendingDecision()
        withClue("resolution pauses for a card selection") {
            (decision is SelectCardsDecision) shouldBe true
        }
        return decision as SelectCardsDecision
    }

    init {
        context("Reckless Crew") {

            test("X counts your Vehicles plus Equipment; each chosen Equipment lands on its own token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Crew")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Raiders' Karve")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withCardOnBattlefield(2, "Raiders' Karve")
                    .withCardOnBattlefield(2, "Bonesplitter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bonesplitter = game.findPermanents("Bonesplitter")
                    .single { game.state.projectedState.getController(it) == game.player1Id }
                val scimitar = game.findPermanent("Leonin Scimitar")!!

                game.castSpell(1, "Reckless Crew").error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Dwarf Berserker Token")
                withClue("one Vehicle + two Equipment you control = three tokens (opponent's don't count)") {
                    tokens shouldHaveSize 3
                }

                val equipmentPick = game.pendingSelection()
                equipmentPick.minSelections shouldBe 0
                game.submitDecision(CardsSelectedResponse(equipmentPick.id, listOf(bonesplitter, scimitar)))

                // First Equipment: any of the three fresh tokens.
                val firstPick = game.pendingSelection()
                val firstToken = tokens.first()
                game.submitDecision(CardsSelectedResponse(firstPick.id, listOf(firstToken)))

                // Second Equipment: the token that already got one is no longer offered.
                val secondPick = game.pendingSelection()
                withClue("a token can't receive a second Equipment from Reckless Crew") {
                    secondPick.options shouldNotContain firstToken
                }
                val secondToken = secondPick.options.first()
                game.submitDecision(CardsSelectedResponse(secondPick.id, listOf(secondToken)))
                game.resolveStack()

                val homes = listOf(game.attachedTo(bonesplitter), game.attachedTo(scimitar))
                withClue("Bonesplitter moved off Grizzly Bears; the two Equipment are on two different tokens") {
                    homes.forEach { it shouldNotBe null; tokens shouldContain it!! }
                    homes.toSet() shouldHaveSize 2
                }
                val bare = tokens.single { it !in homes }
                game.state.projectedState.getPower(bare) shouldBe 2
            }

            test("you may decline to attach, leaving the Equipment where it is") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Crew")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Leonin Scimitar")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scimitar = game.findPermanent("Leonin Scimitar")!!

                game.castSpell(1, "Reckless Crew").error shouldBe null
                game.resolveStack()

                game.findPermanents("Dwarf Berserker Token") shouldHaveSize 1
                val pick = game.pendingSelection()
                game.submitDecision(CardsSelectedResponse(pick.id, emptyList()))
                game.resolveStack()

                game.attachedTo(scimitar) shouldBe null
                game.getPendingDecision() shouldBe null
            }

            test("with no Vehicles or Equipment, X is 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Crew")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Reckless Crew").error shouldBe null
                game.resolveStack()

                game.findPermanents("Dwarf Berserker Token").shouldBeEmpty()
                game.getPendingDecision() shouldBe null
            }
        }
    }
}
