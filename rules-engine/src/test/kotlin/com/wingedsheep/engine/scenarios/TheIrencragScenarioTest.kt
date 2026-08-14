package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for The Irencrag (WOE #248) — {2} Legendary Artifact, Rare.
 *
 * "{T}: Add {C}.
 *  Whenever a legendary creature you control enters, you may have The Irencrag become a legendary
 *  Equipment artifact named Everflame, Heroes' Legacy. If you do, it gains equip {3} and
 *  'Equipped creature gets +3/+3' and loses all other abilities."
 *
 * Focus: the two capabilities the transform needed from
 * [com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect] — `name` (Layer 3 / CR 612.8) and
 * `grantedStaticAbilities`. The latter is the load-bearing one: a *static* ability has to project
 * to do anything, so it rides the permanent's own `ContinuousEffectSourceComponent` rather than the
 * point-of-use granted-static record — and it has to survive this same effect's Layer 6 ability
 * wipe, which it does because a source's own continuous effects are exempt from its own
 * `RemoveAllAbilities`.
 */
class TheIrencragScenarioTest : ScenarioTestBase() {

    init {
        context("The Irencrag") {

            // Board: The Irencrag out, a cheap legendary creature in hand to trigger it, a vanilla
            // creature to equip afterwards, and enough mana for both the legend and equip {3}.
            fun board() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "The Irencrag")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Obyra, Dreaming Duelist")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Cast the legendary creature and answer its enters-trigger's "you may" with [accept].
            fun ScenarioTestBase.TestGame.playLegendAndAnswer(accept: Boolean) {
                castSpell(1, "Obyra, Dreaming Duelist")
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
                answerYesNo(accept)
                resolveStack()
            }

            test("declining the trigger leaves The Irencrag a plain legendary artifact") {
                val game = board()
                val irencrag = game.findPermanent("The Irencrag")!!

                game.playLegendAndAnswer(accept = false)

                val projected = game.state.projectedState
                withClue("name is unchanged when the may-trigger is declined") {
                    (projected.getName(irencrag) ?: "The Irencrag") shouldBe "The Irencrag"
                }
                withClue("still not an Equipment") {
                    projected.hasSubtype(irencrag, "Equipment") shouldBe false
                }
                withClue("keeps its printed abilities") {
                    projected.hasLostAllAbilities(irencrag) shouldBe false
                }
            }

            test("accepting renames it, makes it a legendary Equipment, and wipes its printed abilities") {
                val game = board()
                val irencrag = game.findPermanent("The Irencrag")!!

                game.playLegendAndAnswer(accept = true)

                val projected = game.state.projectedState
                withClue("CR 612.8 — it has only the new name") {
                    projected.getName(irencrag) shouldBe "Everflame, Heroes' Legacy"
                }
                withClue("it is an Equipment artifact") {
                    projected.hasSubtype(irencrag, "Equipment") shouldBe true
                    projected.hasType(irencrag, CardType.ARTIFACT.name) shouldBe true
                }
                withClue("LEGENDARY is a supertype, so replacing the card types leaves it in place") {
                    projected.hasType(irencrag, "LEGENDARY") shouldBe true
                }
                withClue("loses all other abilities — the printed {T}: Add {C} is gone") {
                    projected.hasLostAllAbilities(irencrag) shouldBe true
                }
            }

            test("the granted equip {3} attaches it and the granted static pumps the equipped creature") {
                val game = board()
                val irencrag = game.findPermanent("The Irencrag")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.playLegendAndAnswer(accept = true)

                withClue("+3/+3 only applies once it is actually attached") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                // Equip {3} arrives through the durable granted-activated-ability record, so it is
                // still enumerated even though the permanent has lost all its printed abilities.
                val equip = game.getLegalActions(1).single { info ->
                    val a = info.action
                    a is ActivateAbility && a.sourceId == irencrag
                }
                val equipAction = equip.action as ActivateAbility
                val result = game.execute(equipAction.copy(targets = listOf(ChosenTarget.Permanent(bears))))
                withClue("equip activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("equipped creature gets +3/+3 — the granted static projects through Layer 7c") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
            }
        }
    }
}
