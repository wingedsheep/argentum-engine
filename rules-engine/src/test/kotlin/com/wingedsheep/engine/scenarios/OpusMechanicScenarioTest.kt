package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.opus
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for the `opus { }` ability-word builder (Secrets of Strixhaven). Opus is an
 * ability word, so the mechanic is a single spell-cast triggered ability whose 5+ mana tier reads
 * [com.wingedsheep.sdk.scripting.values.ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL] (the mana
 * spent on the *triggering* spell).
 *
 * Expressive Firedancer (a real `alsoIfFiveOrMore` card) is exercised in
 * [SosTriggerShapeCardsScenarioTest]; this file pins the parts that card doesn't reach: the
 * `insteadIfFiveOrMore` "bonus replaces base" semantics, the exact 5-mana boundary, and a targeted
 * Opus whose single chosen target carries across both tiers.
 */
class OpusMechanicScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        // +1/+1 normally; +2/+2 *instead* when 5+ mana was spent (the bonus replaces the base).
        cardRegistry.register(
            card("Opus Instead Tester") {
                manaCost = "{1}{R}"
                colorIdentity = "R"
                typeLine = "Creature — Human Wizard"
                power = 2
                toughness = 2
                opus {
                    effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
                    insteadIfFiveOrMore = Effects.ModifyStats(2, 2, EffectTarget.Self)
                }
            }
        )

        // Targeted Opus: the chosen creature gets +1/+1, or +2/+2 instead at 5+ mana.
        cardRegistry.register(
            card("Opus Targeted Tester") {
                manaCost = "{1}{U}"
                colorIdentity = "U"
                typeLine = "Creature — Djinn Wizard"
                power = 1
                toughness = 3
                opus {
                    val pick = target("creature", Targets.Creature)
                    effect = Effects.ModifyStats(1, 1, pick)
                    insteadIfFiveOrMore = Effects.ModifyStats(2, 2, pick)
                }
            }
        )

        context("Opus — insteadIfFiveOrMore replaces the base effect") {

            test("a 1-mana spell applies the base +1/+1 (5+ tier not reached)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Opus Instead Tester") // 2/2
                    .withCardInHand(1, "Lightning Bolt") // {R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tester = game.findPermanent("Opus Instead Tester")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("1 mana spent → base +1/+1 only → 3/3") {
                    projector.getProjectedPower(game.state, tester) shouldBe 3
                    projector.getProjectedToughness(game.state, tester) shouldBe 3
                }
            }

            test("a 4-mana spell still applies only the base (boundary: 4 < 5)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Opus Instead Tester") // 2/2
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tester = game.findPermanent("Opus Instead Tester")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Blaze X=3 → {3}{R} → 4 mana spent.
                game.castXSpell(1, "Blaze", xValue = 3, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("4 mana spent → base +1/+1 only → 3/3") {
                    projector.getProjectedPower(game.state, tester) shouldBe 3
                    projector.getProjectedToughness(game.state, tester) shouldBe 3
                }
            }

            test("a 5-mana spell applies +2/+2 INSTEAD (not stacked with the base)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Opus Instead Tester") // 2/2
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tester = game.findPermanent("Opus Instead Tester")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Blaze X=4 → {4}{R} → 5 mana spent (boundary).
                game.castXSpell(1, "Blaze", xValue = 4, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("5 mana spent → +2/+2 instead → 4/4 (NOT 5/5, base is replaced)") {
                    projector.getProjectedPower(game.state, tester) shouldBe 4
                    projector.getProjectedToughness(game.state, tester) shouldBe 4
                }
            }
        }

        context("Opus — a single chosen target carries across both tiers") {

            test("at 1 mana the targeted creature gets +1/+1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Opus Targeted Tester") // 1/3
                    .withCardOnBattlefield(1, "Grizzly Bears") // the creature to buff
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Glory Seeker") // a creature for Bolt to target
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val gloryseeker = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Lightning Bolt", targetId = gloryseeker).error shouldBe null
                game.resolveStack() // Opus trigger asks for its target creature
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("1 mana spent → targeted Bears gets +1/+1 → 3/3") {
                    projector.getProjectedPower(game.state, bears) shouldBe 3
                    projector.getProjectedToughness(game.state, bears) shouldBe 3
                }
            }

            test("at 5 mana the SAME targeted creature gets +2/+2 instead") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Opus Targeted Tester") // 1/3
                    .withCardOnBattlefield(1, "Grizzly Bears") // the creature to buff
                    .withCardInHand(1, "Blaze") // {X}{R}
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val gloryseeker = game.findPermanent("Glory Seeker")!!

                game.castXSpell(1, "Blaze", xValue = 4, targetId = gloryseeker).error shouldBe null
                game.resolveStack() // Opus trigger asks for its target creature
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("5 mana spent → targeted Bears gets +2/+2 instead → 4/4") {
                    projector.getProjectedPower(game.state, bears) shouldBe 4
                    projector.getProjectedToughness(game.state, bears) shouldBe 4
                }
            }
        }
    }
}
