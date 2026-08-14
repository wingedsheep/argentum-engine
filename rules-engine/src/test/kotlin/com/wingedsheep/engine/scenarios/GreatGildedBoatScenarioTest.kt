package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.GreatGildedBoat
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Great Gilded Boat (HOB #42) — {2}{U} Artifact — Vehicle 4/4.
 *
 * "Whenever you attack, recruit." + "Crew 2"
 *
 * "Whenever you attack" is the once-per-combat declare-attackers trigger, so it fires off someone
 * else's attack while the Boat sits uncrewed on the battlefield — the case worth pinning, since a
 * per-attacker reading would make an uncrewed Vehicle do nothing at all.
 */
class GreatGildedBoatScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(GreatGildedBoat)

        context("Great Gilded Boat") {

            test("an attack recruits even though the uncrewed Vehicle stays home") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Great Gilded Boat")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger should pause for recruit's discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val bolt = game.findCardsInHand(1, "Lightning Bolt").single()
                game.selectCards(listOf(bolt))
                game.resolveStack()

                withClue("recruit drew the Forest") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("the discarded nonland is in the graveyard") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("the nonland discard mints one Human Soldier token") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
                withClue("the Vehicle itself never attacked — it is not a creature uncrewed") {
                    game.state.projectedState.isCreature(game.findPermanent("Great Gilded Boat")!!) shouldBe false
                }
            }
        }
    }
}
