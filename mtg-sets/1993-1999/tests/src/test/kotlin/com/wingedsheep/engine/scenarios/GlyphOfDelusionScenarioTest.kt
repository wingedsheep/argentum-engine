package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glyph of Delusion (LEG #60) — "Put X glyph counters on target creature that target Wall blocked
 * this turn, where X is the power of that blocked creature. The creature gains 'This creature
 * doesn't untap during your untap step if it has a glyph counter on it' and 'At the beginning of
 * your upkeep, remove a glyph counter from this creature.'"
 *
 * Player 2 attacks with Grizzly Bears and Hill Giant; Wall of Wood blocks the Bears and Wall of
 * Stone blocks the Giant. The creature target is constrained by the Wall target: only a creature
 * that Wall blocked this turn is legal.
 */
class GlyphOfDelusionScenarioTest : ScenarioTestBase() {

    init {
        context("Glyph of Delusion") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Wall Player", "Attacker")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Glyph of Delusion")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Mountain")
                }
                return builder.build()
            }

            fun TestGame.attackAndBlock() {
                passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                declareBlockers(
                    mapOf("Wall of Wood" to listOf("Grizzly Bears"), "Wall of Stone" to listOf("Hill Giant"))
                ).error shouldBe null
                passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            }

            fun TestGame.castGlyph(wall: String, creature: String) = run {
                if (state.priorityPlayerId == player2Id) passPriority()
                val glyph = state.getHand(player1Id).first {
                    state.getEntity(it)?.get<CardComponent>()?.name == "Glyph of Delusion"
                }
                execute(
                    CastSpell(
                        player1Id, glyph,
                        listOf(ChosenTarget.Permanent(findPermanent(wall)!!), ChosenTarget.Permanent(findPermanent(creature)!!))
                    )
                )
            }

            fun TestGame.glyphs(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.GLYPH) ?: 0

            fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

            test("puts glyph counters equal to the blocked creature's power, which then hold it tapped") {
                val game = setup()
                game.attackAndBlock()
                game.castGlyph("Wall of Stone", "Hill Giant").error shouldBe null
                game.resolveStack()
                val giant = game.findPermanent("Hill Giant")!!

                withClue("Hill Giant has power 3") { game.glyphs(giant) shouldBe 3 }

                // Player 1's turn: not the Giant controller's upkeep, so no counter comes off.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                game.glyphs(giant) shouldBe 3

                // Player 2's turn: the Giant stays tapped through its controller's untap step, then
                // its upkeep trigger removes one counter.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("doesn't untap while it has a glyph counter") { game.isTapped(giant) shouldBe true }
                withClue("its own upkeep removed one glyph counter") { game.glyphs(giant) shouldBe 2 }
                withClue("the Bears weren't touched and untapped normally") {
                    game.isTapped(game.findPermanent("Grizzly Bears")!!) shouldBe false
                }
            }

            test("the last glyph counter coming off lets it untap again") {
                val game = setup()
                game.attackAndBlock()
                game.castGlyph("Wall of Wood", "Grizzly Bears").error shouldBe null
                game.resolveStack()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.glyphs(bears) shouldBe 2

                // Player 2's next turn: stays tapped, 2 -> 1.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.isTapped(bears) shouldBe true
                game.glyphs(bears) shouldBe 1

                // Player 2's turn after that: stays tapped (it still had one in the untap step), 1 -> 0.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.isTapped(bears) shouldBe true
                game.glyphs(bears) shouldBe 0

                // And the turn after, with no glyph counter, it untaps.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.isTapped(bears) shouldBe false
            }

            test("the creature must be one that Wall blocked this turn") {
                val game = setup()
                game.attackAndBlock()
                withClue("Wall of Wood blocked the Bears, not the Giant") {
                    game.castGlyph("Wall of Wood", "Hill Giant").error shouldNotBe null
                }
                withClue("the first target must be a Wall") {
                    game.castGlyph("Grizzly Bears", "Hill Giant").error shouldNotBe null
                }
            }

            test("without blocks there is no legal pair of targets") {
                val game = setup()
                game.castGlyph("Wall of Wood", "Grizzly Bears").error shouldNotBe null
            }
        }
    }
}
