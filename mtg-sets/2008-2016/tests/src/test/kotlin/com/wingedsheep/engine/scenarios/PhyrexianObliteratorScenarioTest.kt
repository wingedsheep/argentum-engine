package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lea.cards.HillGiant
import com.wingedsheep.mtg.sets.definitions.nph.cards.PhyrexianObliterator
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phyrexian Obliterator (NPH) — "Whenever a source deals damage to this creature, that source's
 * controller sacrifices that many permanents of their choice."
 *
 * Every test checks *who* sacrifices (the damage source's controller, never the Obliterator's) and
 * that they choose which permanents go.
 */
class PhyrexianObliteratorScenarioTest : ScenarioTestBase() {

    private val bolt = card("Bolt Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Bolt Test deals 3 damage to target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(3, t)
        }
    }

    private fun board(active: Int): ScenarioTestBase.ScenarioBuilder {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Phyrexian Obliterator", summoningSickness = false)
            .withCardOnBattlefield(1, "Swamp")
            .withCardOnBattlefield(1, "Swamp")
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
            .withActivePlayer(active)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(4) { builder.withCardOnBattlefield(2, "Mountain") }
        repeat(8) {
            builder.withCardInLibrary(1, "Swamp")
            builder.withCardInLibrary(2, "Mountain")
        }
        return builder
    }

    init {
        cardRegistry.register(PhyrexianObliterator)
        cardRegistry.register(HillGiant)
        cardRegistry.register(bolt)

        context("Phyrexian Obliterator") {

            test("a burn spell makes its caster sacrifice that many permanents of their choice") {
                val game = board(active = 2).withCardInHand(2, "Bolt Test").build()
                val obliterator = game.findPermanent("Phyrexian Obliterator")!!
                val giant = game.findPermanent("Hill Giant")!!
                val mountains = game.findPermanents("Mountain")

                game.castSpell(2, "Bolt Test", targetId = obliterator).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses, from their own permanents") {
                    decision.playerId shouldBe game.player2Id
                    decision.minSelections shouldBe 3
                    decision.maxSelections shouldBe 3
                    decision.options.size shouldBe 5
                    decision.options shouldNotContain obliterator
                }
                game.selectCards(listOf(giant, mountains[0], mountains[1])).error shouldBe null
                game.resolveStack()

                withClue("the chosen three are gone, the rest remain") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.findPermanents("Mountain").size shouldBe 2
                }
                withClue("the Obliterator's controller loses nothing; the 5/5 survives 3 damage") {
                    game.isOnBattlefield("Phyrexian Obliterator") shouldBe true
                    game.findPermanents("Swamp").size shouldBe 2
                }
            }

            test("blocking: the attacker dies first, then its controller sacrifices from what is left") {
                val game = board(active = 2).build()
                val obliterator = game.findPermanent("Phyrexian Obliterator")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Phyrexian Obliterator" to listOf("Hill Giant")))
                    .error shouldBe null

                var guard = 0
                while (!game.hasPendingDecision() && guard++ < 20) game.passPriority()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the Hill Giant was destroyed by combat damage before the choice (ruling)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    decision.playerId shouldBe game.player2Id
                    decision.minSelections shouldBe 3
                    decision.options.size shouldBe 4
                    decision.options shouldNotContain obliterator
                }
                val keep = decision.options.first()
                game.selectCards(decision.options.drop(1)).error shouldBe null

                withClue("one Mountain survives — the one the opponent chose to keep") {
                    game.findPermanents("Mountain") shouldBe listOf(keep)
                }
                withClue("the Obliterator and its controller's permanents are untouched") {
                    game.isOnBattlefield("Phyrexian Obliterator") shouldBe true
                    game.findPermanents("Swamp").size shouldBe 2
                }
            }
        }
    }
}
