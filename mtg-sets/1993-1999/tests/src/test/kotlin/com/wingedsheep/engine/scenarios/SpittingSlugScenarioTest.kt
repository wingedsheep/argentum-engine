package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Spitting Slug (DRK #88).
 *
 * {1}{G}{G} Creature — Slug 2/4
 * "Whenever this creature blocks or becomes blocked, you may pay {1}{G}. If you do, this creature
 *  gains first strike until end of turn. Otherwise, each creature blocking or blocked by this
 *  creature gains first strike until end of turn."
 *
 * Both branches are checked through the layer projection *and* through who dies, plus the
 * once-per-combat firing: a Slug blocked by two creatures asks for {1}{G} exactly once.
 */
class SpittingSlugScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Spitting Slug") {

            test("paying {1}{G} gives the Slug first strike and leaves the attacker without it") {
                // The Slug blocks a 2/2. With first strike it kills the Bears before they strike
                // back, so it takes no damage at all.
                val game = scenario()
                    .withPlayers("Blocker", "Attacker")
                    .withCardOnBattlefield(1, "Spitting Slug")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Spitting Slug" to listOf("Grizzly Bears"))).error shouldBe null
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                val slug = game.findPermanent("Spitting Slug")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = projector.project(game.state)
                withClue("the paid branch keeps first strike on the Slug") {
                    projected.hasKeyword(slug, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("and does not hand it to the attacker") {
                    projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("2 first-strike damage kills the 2/2 before it can deal any") {
                    game.findPermanent("Grizzly Bears").shouldBeNull()
                }
                withClue("so the 2/4 Slug survives untouched") {
                    game.findPermanent("Spitting Slug").shouldNotBeNull()
                }
            }

            test("declining hands first strike to the creature it is blocking instead") {
                // The Slug blocks a 6/4. First strike on the *attacker* kills the 2/4 Slug before
                // it deals its 2 damage, so the attacker walks away undamaged.
                val game = scenario()
                    .withPlayers("Blocker", "Attacker")
                    .withCardOnBattlefield(1, "Spitting Slug")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Spitting Slug" to listOf("Craw Wurm"))).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                val slug = game.findPermanent("Spitting Slug")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val projected = projector.project(game.state)
                withClue("the unpaid branch gives first strike to the blocked attacker") {
                    projected.hasKeyword(wurm, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("and takes it back off the Slug") {
                    projected.hasKeyword(slug, Keyword.FIRST_STRIKE) shouldBe false
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("6 first-strike damage kills the 2/4 Slug before it strikes") {
                    game.findPermanent("Spitting Slug").shouldBeNull()
                }
                withClue("so the 6/4 attacker takes nothing") {
                    game.findPermanent("Craw Wurm").shouldNotBeNull()
                }
            }

            test("blocked by two creatures, the trigger still fires exactly once") {
                // The printed wording has no "by [filter]" clause, so it is one trigger for the
                // whole combat — not one per blocker. Declining once must hand first strike to
                // *both* blockers.
                val game = scenario()
                    .withPlayers("Attacker", "Blocker")
                    .withCardOnBattlefield(1, "Spitting Slug")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hurloon Minotaur")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Spitting Slug" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Grizzly Bears" to listOf("Spitting Slug"),
                        "Hurloon Minotaur" to listOf("Spitting Slug")
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("one trigger, so there is no second {1}{G} prompt") {
                    game.state.pendingDecision.shouldBeNull()
                }

                val projected = projector.project(game.state)
                withClue("both blockers are 'blocking this creature' and both get first strike") {
                    projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FIRST_STRIKE) shouldBe true
                    projected.hasKeyword(game.findPermanent("Hurloon Minotaur")!!, Keyword.FIRST_STRIKE) shouldBe true
                }
            }
        }
    }
}
