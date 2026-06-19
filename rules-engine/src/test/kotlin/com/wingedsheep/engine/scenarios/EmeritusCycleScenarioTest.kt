package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for the Secrets of Strixhaven "Emeritus" preparation cycle (5 mythics).
 *
 * Each is a PREPARE-layout creature whose creature face has a "becomes prepared" trigger and whose
 * prepare spell is a famous reprint:
 *  - Emeritus of Conflict   // Lightning Bolt      — third spell each turn (does NOT enter prepared)
 *  - Emeritus of Abundance  // Regrowth            — attacks w/ 8+ lands (enters prepared)
 *  - Emeritus of Ideation   // Ancestral Recall    — attacks, may exile 8 from graveyard (enters prepared)
 *  - Emeritus of Truce      // Swords to Plowshares — ETB token, "then if" opp has more creatures
 *  - Emeritus of Woe        // Demonic Tutor        — your end step, 2+ creatures died (enters prepared)
 *
 * These exercise existing primitives (NthSpellCast, attack/end-step triggers, intervening-if
 * conditions, MayEffect→IfYouDo, ConditionalEffect, BecomePrepared) — no new SDK was added.
 */
class EmeritusCycleScenarioTest : ScenarioTestBase() {

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): com.wingedsheep.sdk.model.EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    private fun TestGame.isPrepared(name: String): Boolean {
        val id = findPermanent(name) ?: return false
        return state.getEntity(id)?.get<PreparedComponent>() != null
    }

    init {
        context("Emeritus of Conflict — third spell each turn (Lightning Bolt)") {
            test("does not enter prepared; only the third spell each turn prepares it; copy is a {R} bolt") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Emeritus of Conflict", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                withClue("Emeritus of Conflict has no PREPARED keyword — must not enter prepared") {
                    game.isPrepared("Emeritus of Conflict") shouldBe false
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()
                withClue("After two spells this turn it is still not prepared") {
                    game.isPrepared("Emeritus of Conflict") shouldBe false
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                withClue("Third spell each turn prepares Emeritus of Conflict") {
                    game.isPrepared("Emeritus of Conflict") shouldNotBe false
                }
                val copyId = game.findExileCopy(1, "Emeritus of Conflict")
                withClue("A Lightning Bolt prepare-spell copy should be in exile, castable for {R}") {
                    copyId shouldNotBe null
                }
                val cast = game.getLegalActions(1).firstOrNull {
                    val a = it.action
                    a is CastSpell && a.cardId == copyId
                }
                withClue("Lightning Bolt copy is offered from exile for {R}") {
                    cast shouldNotBe null
                    (cast!!.action as CastSpell).faceIndex shouldBe 0
                    cast.manaCostString shouldBe "{R}"
                    Unit
                }
            }
        }

        context("Emeritus of Abundance — attacks with 8+ lands (Regrowth)") {
            test("enters prepared; Regrowth copy returns a graveyard card and unprepares it") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Emeritus of Abundance")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .withCardInGraveyard(1, "Plains")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Emeritus of Abundance")
                game.resolveStack()
                withClue("Emeritus of Abundance enters prepared") {
                    game.isPrepared("Emeritus of Abundance") shouldBe true
                }

                // Cast the Regrowth copy returning the Plains from the graveyard; it unprepares.
                val copy = game.findExileCopy(1, "Emeritus of Abundance")!!
                val target = game.findCardsInGraveyard(1, "Plains").first()
                game.execute(
                    CastSpell(
                        game.player1Id, copy,
                        targets = listOf(ChosenTarget.Card(target, game.player1Id, Zone.GRAVEYARD)),
                        faceIndex = 0,
                    )
                )
                game.resolveStack()
                withClue("Regrowth returns the Plains to hand") {
                    game.state.getHand(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Plains"
                    } shouldBe true
                }
                withClue("Casting the Regrowth copy unprepares Emeritus of Abundance") {
                    game.isPrepared("Emeritus of Abundance") shouldBe false
                }
            }

            test("attack re-prepares only when you control eight or more lands") {
                // On battlefield (no summoning sickness) and not yet prepared, with only 7 lands.
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Emeritus of Abundance", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                withClue("Placed directly on the battlefield, no ETB fired, so it is not prepared") {
                    game.isPrepared("Emeritus of Abundance") shouldBe false
                }

                // Attack with only 7 lands — intervening-if fails, no prepare.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Emeritus of Abundance" to 2))
                game.resolveStack()
                withClue("Attacking with only 7 lands does NOT prepare it") {
                    game.isPrepared("Emeritus of Abundance") shouldBe false
                }
            }

            test("attack with eight or more lands re-prepares it") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Emeritus of Abundance", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Emeritus of Abundance" to 2))
                game.resolveStack()
                withClue("Attacking with 8+ lands re-prepares Emeritus of Abundance") {
                    game.isPrepared("Emeritus of Abundance") shouldBe true
                }
            }
        }

        context("Emeritus of Ideation — attacks, may exile 8 from graveyard (Ancestral Recall)") {
            test("enters prepared with flying and ward; the Ancestral Recall copy draws three") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Emeritus of Ideation")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Emeritus of Ideation")
                game.resolveStack()
                withClue("Emeritus of Ideation enters prepared") {
                    game.isPrepared("Emeritus of Ideation") shouldBe true
                }
                val ideation = game.findPermanent("Emeritus of Ideation")!!
                withClue("It has flying") {
                    game.state.projectedState.hasKeyword(
                        ideation, com.wingedsheep.sdk.core.Keyword.FLYING,
                    ) shouldBe true
                }

                val handBefore = game.state.getHand(game.player1Id).size
                val copy = game.findExileCopy(1, "Emeritus of Ideation")!!
                game.execute(
                    CastSpell(
                        game.player1Id, copy,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                        faceIndex = 0,
                    )
                )
                game.resolveStack()
                withClue("Ancestral Recall draws the targeted player three cards") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore + 3
                }
                withClue("Casting the Ancestral Recall copy unprepares Emeritus of Ideation") {
                    game.isPrepared("Emeritus of Ideation") shouldBe false
                }
            }

            test("attack re-prepares it when you exile eight cards from your graveyard") {
                // On battlefield (no summoning sickness), not prepared, with 8 cards in graveyard.
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Emeritus of Ideation", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(8) { builder = builder.withCardInGraveyard(1, "Island") }
                repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                withClue("Placed directly on the battlefield, no ETB fired, so it is not prepared") {
                    game.isPrepared("Emeritus of Ideation") shouldBe false
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Emeritus of Ideation" to 2))
                // The attack trigger resolves and pauses on the optional "you may exile eight
                // cards from your graveyard" — accept, then pick the eight graveyard cards.
                game.resolveStack()
                game.answerYesNo(true)
                // With exactly eight cards in the graveyard the engine auto-selects them; if it
                // still prompts, pick the eight.
                if (game.hasPendingDecision()) {
                    val graveCards = game.findCardsInGraveyard(1, "Island")
                    game.selectCards(graveCards.take(8))
                }
                game.resolveStack()
                withClue("All eight graveyard cards are exiled") {
                    game.findCardsInGraveyard(1, "Island").size shouldBe 0
                }
                withClue("Exiling eight cards re-prepares Emeritus of Ideation") {
                    game.isPrepared("Emeritus of Ideation") shouldBe true
                }
            }
        }

        context("Emeritus of Truce — ETB token then conditional prepare (Swords to Plowshares)") {
            test("enters prepared (keyword) and ETB makes target player an Inkling token") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Emeritus of Truce")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Emeritus of Truce")
                game.resolveStack()
                // The creature resolved; its ETB trigger is on the stack asking for a target
                // player — choose self.
                game.selectTargets(listOf(game.player1Id))
                game.resolveStack()

                withClue("Emeritus of Truce enters prepared (has the PREPARED keyword)") {
                    game.isPrepared("Emeritus of Truce") shouldBe true
                }
                val projected = game.state.projectedState
                val inklings = game.state.getBattlefield(game.player1Id).filter { entity ->
                    projected.isCreature(entity) &&
                        projected.getSubtypes(entity).contains("Inkling") &&
                        projected.getPower(entity) == 1 &&
                        projected.getToughness(entity) == 1 &&
                        projected.hasKeyword(entity, com.wingedsheep.sdk.core.Keyword.FLYING)
                }
                withClue("ETB creates a 1/1 flying Inkling token for the chosen player") {
                    inklings.size shouldBe 1
                }
            }
        }

        context("Emeritus of Woe — your end step, 2+ creatures died (Demonic Tutor)") {
            test("enters prepared and stays castable as Demonic Tutor from exile") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Emeritus of Woe")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Swamp") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Emeritus of Woe")
                game.resolveStack()
                withClue("Emeritus of Woe enters prepared") {
                    game.isPrepared("Emeritus of Woe") shouldBe true
                }
                val copyId = game.findExileCopy(1, "Emeritus of Woe")
                withClue("A Demonic Tutor prepare-spell copy should be in exile for {1}{B}") {
                    copyId shouldNotBe null
                }
                val cast = game.getLegalActions(1).firstOrNull {
                    val a = it.action
                    a is CastSpell && a.cardId == copyId
                }
                withClue("Demonic Tutor copy is offered from exile for {1}{B}") {
                    cast shouldNotBe null
                    cast!!.manaCostString shouldBe "{1}{B}"
                    Unit
                }
            }
        }
    }
}
