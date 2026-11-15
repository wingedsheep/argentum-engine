package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A copy of an instant or sorcery spell and its "as you cast this spell" captures (CR 601.2i).
 *
 * A copy isn't cast (CR 707.10), so a capture that describes the cast — "if you controlled a Dragon
 * as you cast this spell" — reads as absent on the copy. The DTK Dragon-bonus ruling: "the controller
 * of the copy will get the 'Dragon bonus' only if a Dragon card was revealed as an additional cost.
 * The copy wasn't cast, so whether you controlled a Dragon won't matter." The revealed card is a cost
 * decision, which a copy does inherit (CR 707.10).
 *
 * The exception is a capture that is the pool of a division announced at cast (CR 601.2d): the copy
 * inherits the division, so it inherits the amount divided too (Monstrous Onslaught, Undercity
 * Upheaval). Each copy here is Fork's, keeping the original's targets.
 */
class SpellCopyCastTimeCaptureTest : ScenarioTestBase() {

    /**
     * Player 1 holds priority and casts Fork at [spellName], keeping the copy's targets; returns the
     * copy once it is on the stack above the original.
     */
    private fun TestGame.forkKeepingTargets(spellName: String): EntityId {
        castSpellTargetingStackSpell(1, "Fork", spellName).error shouldBe null
        var guard = 0
        while (state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) passPriority()
        val question = state.pendingDecision as ChooseTargetsDecision
        submitDecision(TargetsResponse(question.id, emptyMap())).error shouldBe null
        return state.stack.single { state.getEntity(it)?.has<CopyOfComponent>() == true }
    }

    init {
        context("Draconic Roar's Dragon bonus") {
            // Craw Wurm (6/4) survives the copy's 3, so the original still has its target.
            fun board(dragonOnBattlefield: Boolean = false, dragonInHand: Boolean = false): TestGame {
                var b = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Draconic Roar")
                    .withCardInHand(1, "Fork")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (dragonOnBattlefield) b = b.withCardOnBattlefield(1, "Shivan Dragon")
                if (dragonInHand) b = b.withCardInHand(1, "Shivan Dragon")
                return b.build()
            }

            test("a copy of a spell cast while controlling a Dragon gets no bonus") {
                val game = board(dragonOnBattlefield = true)
                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Draconic Roar", wurm).error shouldBe null
                val copy = game.forkKeepingTargets("Draconic Roar")
                withClue("the copy carries none of the original's cast-time captures") {
                    game.state.getEntity(copy)!!.get<SpellOnStackComponent>()!!.castTimeValues shouldBe emptyMap()
                }

                game.resolveStack()

                withClue("only the original — which was cast — deals 3 to the creature's controller") {
                    game.getLifeTotal(2) shouldBe 17
                }
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
            }

            test("a copy of a spell cast revealing a Dragon card keeps the bonus") {
                val game = board(dragonInHand = true)
                val wurm = game.findPermanent("Craw Wurm")!!
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Draconic Roar").single(),
                        targets = listOf(ChosenTarget.Permanent(wurm)),
                        declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                        additionalCostPayment = AdditionalCostPayment(
                            revealedCards = game.findCardsInHand(1, "Shivan Dragon"),
                        ),
                    )
                ).error shouldBe null
                game.forkKeepingTargets("Draconic Roar")
                game.resolveStack()

                withClue("the reveal is a cost decision the copy inherits: both deal 3 to the controller") {
                    game.getLifeTotal(2) shouldBe 14
                }
            }
        }

        context("a division's pool announced at cast") {
            test("a copy of Monstrous Onslaught with one target deals the captured X, not 0") {
                // Hill Giant is the greatest power: X = 3. Craw Wurm (6/4) survives one 3 but not two.
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Monstrous Onslaught")
                    .withCardInHand(1, "Fork")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Monstrous Onslaught", wurm).error shouldBe null
                val copy = game.forkKeepingTargets("Monstrous Onslaught")
                game.state.getEntity(copy)!!.get<SpellOnStackComponent>()!!.castTimeValues shouldBe mapOf("x" to 3)

                game.resolveStack()

                withClue("the copy's 3 and the original's 3 kill the 6/4") {
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }
            }

            test("a copy of Undercity Upheaval with one target places the captured X, not 0") {
                // Two creature cards in the graveyard: X = 2.
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Undercity Upheaval")
                    .withCardInHand(1, "Fork")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Ornithopter")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                game.castSpell(1, "Undercity Upheaval", bears).error shouldBe null
                game.forkKeepingTargets("Undercity Upheaval")

                game.resolveStack()

                fun plusOnes(id: EntityId) =
                    game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("the original's 2 and the copy's 2") { plusOnes(bears) shouldBe 4 }
                plusOnes(elves) shouldBe 0
            }
        }
    }
}
