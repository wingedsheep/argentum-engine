package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lead Pipe (MKM #90) — {B} Artifact — Clue Equipment.
 * "Equipped creature gets +2/+0. / Whenever equipped creature dies, each opponent loses 1 life. /
 * {2}, Sacrifice this Equipment: Draw a card. / Equip {2}"
 *
 * The death trigger is an ATTACHED-bound battlefield→graveyard zone change, which reads its
 * subject through the attachment rather than off the source — the shape that silently no-ops if
 * the binding is wrong. Covered here alongside the static buff and the Clue half.
 */
class LeadPipeScenarioTest : ScenarioTestBase() {

    init {
        context("Lead Pipe") {

            test("equipped creature gets +2/+0 and its death drains each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Lead Pipe", "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val pipe = game.findPermanent("Lead Pipe")!!

                withClue("2/2 base + 2/+0 from the Equipment") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }

                // Bolt the equipped creature — 3 damage to a 4/2 kills it.
                game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the equipped creature died") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // The dies trigger goes on the stack; resolve it.
                game.resolveStack()

                withClue("each opponent lost 1 life; the Equipment's controller did not") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("the Equipment stays on the battlefield, just unattached (CR 704.5m)") {
                    game.findPermanent("Lead Pipe") shouldBe pipe
                }
            }

            test("the Clue half sacrifices for a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lead Pipe")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val pipe = game.findPermanent("Lead Pipe")!!
                // activatedAbilities[0] is the Clue sacrifice; [1] is equip.
                val sacrifice = cardRegistry.getCard("Lead Pipe")!!.script.activatedAbilities[0]

                game.execute(ActivateAbility(game.player1Id, pipe, sacrifice.id)).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the Equipment was sacrificed and a card was drawn") {
                    game.findPermanent("Lead Pipe") shouldBe null
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
