package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Taskmaster, Mercenary Mimic (MSH #232) — {2}{U}{B} Legendary Creature, 3/5, Rare.
 *
 * "Photographic Reflexes — At the beginning of your first main phase, until your next turn,
 *  Taskmaster becomes a copy of up to one target creature on the battlefield or creature card in a
 *  graveyard, except his name is Taskmaster, Mercenary Mimic and he's a legendary Human Mercenary
 *  Villain creature."
 *
 * Focus: the *replacing* reading of a type clause. Absorbing Man, in the same set, says "in addition
 * to his other types" and Taskmaster deliberately does not — CR 205.1a replaces by default and
 * CR 205.1b retains only on exactly that phrase — so his creature types are overridden, not unioned.
 * Also covers the cross-zone target
 * (battlefield **or** graveyard) and `sourceFromAnyZone`.
 */
class TaskmasterMercenaryMimicScenarioTest : ScenarioTestBase() {

    init {
        context("Taskmaster, Mercenary Mimic") {

            // The "until your next turn" case runs three turns, so both libraries need cards —
            // the scenario builder starts them empty and a draw from an empty library ends the
            // game (CR 704.5b), which silently stalls any further turn advance.
            fun board(): ScenarioTestBase.TestGame {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Taskmaster, Mercenary Mimic")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                repeat(5) {
                    builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island")
                }
                return builder.build()
            }

            fun ScenarioTestBase.TestGame.advanceToFirstMainTrigger() {
                var iterations = 0
                while (!hasPendingDecision() &&
                    state.step != Step.PRECOMBAT_MAIN &&
                    iterations++ < 40
                ) {
                    val priority = state.priorityPlayerId ?: break
                    execute(PassPriority(priority))
                }
            }

            /**
             * Advance to the next player's upkeep. Routed through the end step on purpose:
             * `passUntilPhase` returns immediately when the game is already at the requested
             * phase/step, so two upkeep hops in a row would silently be a single hop.
             */
            fun ScenarioTestBase.TestGame.advanceToNextUpkeep() {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            }

            fun ScenarioTestBase.TestGame.copyOnto(targetId: EntityId) {
                advanceToFirstMainTrigger()
                if (hasPendingDecision()) selectTargets(listOf(targetId))
                resolveStack()
            }

            test("copies a creature on the battlefield but keeps his own name and type line") {
                val game = board()
                val taskmaster = game.findPermanent("Taskmaster, Mercenary Mimic")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.copyOnto(bears)

                val card = game.state.getEntity(taskmaster)!!.get<CardComponent>()!!
                withClue("except his name is Taskmaster, Mercenary Mimic") {
                    card.name shouldBe "Taskmaster, Mercenary Mimic"
                }
                withClue("copiable P/T come from the copied creature") {
                    game.state.projectedState.getPower(taskmaster) shouldBe 2
                    game.state.projectedState.getToughness(taskmaster) shouldBe 2
                }
                withClue("his stated type line replaces the copied one — no 'in addition' clause") {
                    card.typeLine.isLegendary shouldBe true
                    card.typeLine.isCreature shouldBe true
                    card.typeLine.subtypes shouldBe setOf(
                        Subtype.HUMAN, Subtype.MERCENARY, Subtype.VILLAIN
                    )
                    card.typeLine.subtypes.contains(Subtype.BEAR) shouldBe false
                }
            }

            test("copying an artifact creature drops the artifact card type") {
                val game = board()
                val taskmaster = game.findPermanent("Taskmaster, Mercenary Mimic")!!
                val ornithopter = game.findPermanent("Ornithopter")!!

                game.copyOnto(ornithopter)

                val card = game.state.getEntity(taskmaster)!!.get<CardComponent>()!!
                withClue("a stated type line replaces (CR 205.1a) — he is a creature and nothing else") {
                    card.typeLine.isCreature shouldBe true
                    card.typeLine.isArtifact shouldBe false
                    card.typeLine.cardTypes shouldBe setOf(CardType.CREATURE)
                }
                withClue("subtypes are replaced too — no Thopter") {
                    card.typeLine.subtypes shouldBe setOf(
                        Subtype.HUMAN, Subtype.MERCENARY, Subtype.VILLAIN
                    )
                }
                withClue("P/T are copiable values, so they still come from Ornithopter") {
                    game.state.projectedState.getPower(taskmaster) shouldBe 0
                    game.state.projectedState.getToughness(taskmaster) shouldBe 2
                }
            }

            test("copies a creature card in a graveyard — the cross-zone target") {
                val game = board()
                val taskmaster = game.findPermanent("Taskmaster, Mercenary Mimic")!!

                game.advanceToFirstMainTrigger()
                val hillGiant = game.findCardsInGraveyard(1, "Hill Giant").first()
                if (game.hasPendingDecision()) {
                    val result = game.selectTargets(listOf(hillGiant))
                    withClue("a graveyard creature card is a legal target: ${result.error}") {
                        result.error shouldBe null
                    }
                }
                game.resolveStack()

                withClue("Hill Giant's copiable values, Taskmaster's name and types") {
                    val card = game.state.getEntity(taskmaster)!!.get<CardComponent>()!!
                    card.name shouldBe "Taskmaster, Mercenary Mimic"
                    game.state.projectedState.getPower(taskmaster) shouldBe 3
                    game.state.projectedState.getToughness(taskmaster) shouldBe 3
                    card.typeLine.subtypes shouldBe setOf(
                        Subtype.HUMAN, Subtype.MERCENARY, Subtype.VILLAIN
                    )
                }
                withClue("the copied card stays in the graveyard — nothing moved") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
            }

            test("the copy lasts until his controller's next turn") {
                val game = board()
                val taskmaster = game.findPermanent("Taskmaster, Mercenary Mimic")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.copyOnto(bears)

                game.advanceToNextUpkeep()
                withClue("still a copy through the opponent's turn") {
                    game.state.projectedState.getPower(taskmaster) shouldBe 2
                }

                game.advanceToNextUpkeep()
                withClue("back to his printed 3/5 after his own untap step") {
                    game.state.projectedState.getPower(taskmaster) shouldBe 3
                    game.state.projectedState.getToughness(taskmaster) shouldBe 5
                }
            }
        }
    }
}
