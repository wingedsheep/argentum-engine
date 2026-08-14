package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Stegron the Dinosaur Man (SPM #95) — {4}{R} Legendary Creature —
 * Dinosaur Villain 5/4.
 *
 *   Menace
 *   Dinosaur Formula — {1}{R}, Discard this card: Until end of turn, target creature you
 *   control gets +3/+1 and becomes a Dinosaur in addition to its other types.
 *
 * Exercises the from-hand activated ability ([activateFromZone] = HAND, cost
 * [Costs.DiscardSelf]): activating it discards Stegron and, until end of turn, gives the
 * targeted creature you control +3/+1 and adds the Dinosaur creature type without removing
 * its existing subtypes.
 */
class StegronTheDinosaurManScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Stegron the Dinosaur Man") {

            test("Dinosaur Formula: discard Stegron, target creature gets +3/+1 and becomes a Dinosaur") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stegron the Dinosaur Man")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Birds of Paradise") // 0/1 Bird you control
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val birds = game.findPermanent("Birds of Paradise")!!
                val handCard = game.findCardsInHand(1, "Stegron the Dinosaur Man").first()
                val abilityId = cardRegistry.getCard("Stegron the Dinosaur Man")!!
                    .activatedAbilities.first().id

                val before = projector.project(game.state)
                withClue("baseline: Birds of Paradise is a 0/1 Bird, not a Dinosaur") {
                    before.getPower(birds) shouldBe 0
                    before.getToughness(birds) shouldBe 1
                    before.hasSubtype(birds, "Bird") shouldBe true
                    before.hasSubtype(birds, "Dinosaur") shouldBe false
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = handCard,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(birds))
                    )
                )
                withClue("activating the from-hand ability targeting your own creature is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("the ability's discard-self cost put Stegron into the graveyard") {
                    game.isInGraveyard(1, "Stegron the Dinosaur Man") shouldBe true
                    game.isInHand(1, "Stegron the Dinosaur Man") shouldBe false
                }

                val after = projector.project(game.state)
                withClue("target got +3/+1 (0/1 -> 3/2)") {
                    after.getPower(birds) shouldBe 3
                    after.getToughness(birds) shouldBe 2
                }
                withClue("target became a Dinosaur in addition to its other types (still a Bird)") {
                    after.isCreature(birds) shouldBe true
                    after.hasSubtype(birds, "Dinosaur") shouldBe true
                    after.hasSubtype(birds, "Bird") shouldBe true
                }
            }
        }
    }
}
