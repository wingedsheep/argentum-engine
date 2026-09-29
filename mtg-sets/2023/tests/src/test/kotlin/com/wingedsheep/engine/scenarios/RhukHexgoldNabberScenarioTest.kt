package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rhuk, Hexgold Nabber (ONE #407) — whenever an equipped creature you control other than Rhuk
 * attacks or dies, you may attach all Equipment attached to that creature to Rhuk.
 *
 * The dies leg is the last-known-information case: by resolution the creature is in the graveyard
 * and the state-based action has unattached its Equipment, so the gather reads the attachments
 * frozen on the zone change (CR 608.2h).
 */
class RhukHexgoldNabberScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.bearsWithTwoEquipment(): ScenarioBuilder = this
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Rhuk, Hexgold Nabber")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
        .withCardAttachedTo(1, "Leonin Scimitar", "Grizzly Bears")

    private fun TestGame.attachedTo(name: String) =
        state.getEntity(findPermanent(name)!!)?.get<AttachedToComponent>()?.targetId

    private fun TestGame.killBears() {
        // Bears are 5/3 with both Equipment — Lightning Bolt kills them.
        val cast = castSpell(1, "Lightning Bolt", findPermanent("Grizzly Bears")!!)
        withClue("Bolt should cast: ${cast.error}") { cast.error shouldBe null }
        if (hasPendingDecision() && getPendingDecision() !is YesNoDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        context("Rhuk, Hexgold Nabber") {
            test("equipped creature dies — all its Equipment moves to Rhuk") {
                val game = scenario()
                    .bearsWithTwoEquipment()
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.killBears()
                withClue("the Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("the dies trigger asks whether to attach") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val rhuk = game.findPermanent("Rhuk, Hexgold Nabber")!!
                withClue("both Equipment that were on the Bears are now on Rhuk") {
                    game.attachedTo("Bonesplitter") shouldBe rhuk
                    game.attachedTo("Leonin Scimitar") shouldBe rhuk
                }
                withClue("Rhuk is 2/2 + 2/0 + 1/1 = 5/3") {
                    game.state.projectedState.getPower(rhuk) shouldBe 5
                    game.state.projectedState.getToughness(rhuk) shouldBe 3
                }
            }

            test("declining leaves the dead creature's Equipment unattached") {
                val game = scenario()
                    .bearsWithTwoEquipment()
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.killBears()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("neither Equipment was attached to anything") {
                    game.attachedTo("Bonesplitter") shouldBe null
                    game.attachedTo("Leonin Scimitar") shouldBe null
                }
            }

            test("only the Equipment that was on the dying creature moves") {
                // Only what was attached when the creature died moves — a third Equipment elsewhere
                // on the battlefield stays where it is.
                val game = scenario()
                    .bearsWithTwoEquipment()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Short Sword", "Hill Giant")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.killBears()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Short Sword stays on the Hill Giant") {
                    game.attachedTo("Short Sword") shouldBe game.findPermanent("Hill Giant")
                }
                withClue("the Bears' Equipment moved to Rhuk") {
                    game.attachedTo("Bonesplitter") shouldBe game.findPermanent("Rhuk, Hexgold Nabber")
                }
            }

            test("equipped creature attacks — its Equipment moves to Rhuk") {
                val game = scenario()
                    .bearsWithTwoEquipment()
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                if (game.getPendingDecision() !is YesNoDecision) game.resolveStack()
                withClue("the attack trigger asks whether to attach") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val rhuk = game.findPermanent("Rhuk, Hexgold Nabber")!!
                game.attachedTo("Bonesplitter") shouldBe rhuk
                game.attachedTo("Leonin Scimitar") shouldBe rhuk
            }

            test("Rhuk attacking equipped does not trigger itself — only another creature does") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Rhuk, Hexgold Nabber")
                    .withCardAttachedTo(1, "Bonesplitter", "Rhuk, Hexgold Nabber")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Rhuk, Hexgold Nabber" to 2)).error shouldBe null
                withClue("no Rhuk trigger for its own attack") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
            }

            test("an unequipped creature dying does not trigger") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Rhuk, Hexgold Nabber")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.killBears()
                withClue("no Rhuk trigger") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
            }
        }
    }
}
