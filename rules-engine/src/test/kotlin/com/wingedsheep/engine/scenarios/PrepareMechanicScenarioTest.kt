package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Prepare / Prepared mechanic (Secrets of Strixhaven), covering
 * Adventurous Eater // Have a Bite and Landscape Painter // Vibrant Idea.
 *
 * A preparation creature enters prepared: its controller gets a copy of the card's prepare spell
 * in exile and may cast that copy (paying its mana cost). Casting the copy unprepares the creature
 * and resolves the prepare spell; the copy ceases to exist. If the creature leaves the battlefield
 * the exiled copy is cleaned up.
 */
class PrepareMechanicScenarioTest : ScenarioTestBase() {

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): com.wingedsheep.sdk.model.EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    init {
        context("Adventurous Eater — Have a Bite") {

            test("entering creates an exiled prepare-spell copy and marks the creature prepared") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Adventurous Eater")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Adventurous Eater")
                game.resolveStack()

                val eater = game.findPermanent("Adventurous Eater")!!
                val prepared = game.state.getEntity(eater)?.get<PreparedComponent>()
                withClue("Adventurous Eater should be prepared on ETB") {
                    prepared shouldNotBe null
                }
                val copyId = game.findExileCopy(1, "Adventurous Eater")
                withClue("A prepare-spell copy should exist in exile") {
                    copyId shouldNotBe null
                }
                prepared!!.exileCopyId shouldBe copyId
            }

            test("casting the prepare-spell copy adds a +1/+1 counter, gains life, and unprepares the creature") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Adventurous Eater")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Adventurous Eater")
                game.resolveStack()

                val eater = game.findPermanent("Adventurous Eater")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val copyId = game.findExileCopy(1, "Adventurous Eater")!!

                // The cast-from-exile enumerator should surface the prepare-spell copy as a {B}
                // cast of face 0 from exile.
                val prepareAction = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is CastSpell && a.cardId == copyId
                }
                withClue("The prepare-spell copy should be offered as a legal cast from exile") {
                    prepareAction shouldNotBe null
                }
                withClue("It should be cast as face 0 (the prepare spell) for {B}") {
                    (prepareAction!!.action as CastSpell).faceIndex shouldBe 0
                    prepareAction.sourceZone shouldBe "EXILE"
                    prepareAction.manaCostString shouldBe "{B}"
                }

                // Cast the prepare-spell copy (face 0) targeting Grizzly Bears.
                game.execute(
                    CastSpell(
                        game.player1Id,
                        copyId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        faceIndex = 0
                    )
                )
                game.resolveStack()

                withClue("Grizzly Bears should have a +1/+1 counter from Have a Bite") {
                    val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                    (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
                }
                withClue("Controller should have gained 1 life") {
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("Adventurous Eater should no longer be prepared") {
                    game.state.getEntity(eater)?.get<PreparedComponent>() shouldBe null
                }
                withClue("The prepare-spell copy should be gone from exile") {
                    game.findExileCopy(1, "Adventurous Eater") shouldBe null
                }
            }
        }

        context("Landscape Painter — Vibrant Idea") {

            test("casting the prepare-spell copy draws two cards and unprepares the creature") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Landscape Painter")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Landscape Painter")
                game.resolveStack()

                val painter = game.findPermanent("Landscape Painter")!!
                val handBefore = game.handSize(1)
                val copyId = game.findExileCopy(1, "Landscape Painter")!!

                game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                game.resolveStack()

                withClue("Vibrant Idea draws two cards") {
                    game.handSize(1) shouldBe handBefore + 2
                }
                withClue("Landscape Painter should no longer be prepared") {
                    game.state.getEntity(painter)?.get<PreparedComponent>() shouldBe null
                }
                withClue("The prepare-spell copy should be gone from exile") {
                    game.findExileCopy(1, "Landscape Painter") shouldBe null
                }
            }
        }

        context("Prepared cleanup") {

            test("the exiled copy is removed when the prepared creature leaves the battlefield") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Landscape Painter")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Landscape Painter")
                game.resolveStack()

                val painter = game.findPermanent("Landscape Painter")!!
                game.findExileCopy(1, "Landscape Painter") shouldNotBe null

                // Destroy the prepared creature (Murder targets the painter).
                game.castSpell(1, "Murder", painter)
                game.resolveStack()

                withClue("Landscape Painter should be gone from the battlefield") {
                    game.findPermanent("Landscape Painter") shouldBe null
                }
                withClue("The prepare-spell copy should be cleaned up once the source leaves the battlefield") {
                    game.findExileCopy(1, "Landscape Painter") shouldBe null
                }
            }
        }
    }
}
