package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mantle of the Ancients (AFC #8) — "When this Aura enters, return any number of target Aura and/or
 * Equipment cards from your graveyard to the battlefield attached to enchanted creature. Enchanted
 * creature gets +1/+1 for each Aura and Equipment attached to it."
 *
 * Pins the three rulings: targets that couldn't be legally attached stay in the graveyard — an
 * Aura whose enchant restriction the creature fails and an Equipment it can't carry alike; and with
 * the Mantle gone before its ability resolves, the creature it was last attached to is "enchanted
 * creature".
 */
class MantleOfTheAncientsScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(1, "Mantle of the Ancients")
        .withCardInGraveyard(1, "Holy Strength")
        .withCardInGraveyard(1, "Bonesplitter")
        .withCardInGraveyard(1, "Konda's Banner")
        .withCardInGraveyard(1, "Erosion")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attachedTo(name: String) =
        findPermanent(name)?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }

    /** Cast the Mantle on the Bears, resolve it, and target every Aura/Equipment card in the graveyard. */
    private fun TestGame.castMantleTargetingEverything() {
        castSpell(1, "Mantle of the Ancients", findPermanent("Grizzly Bears")!!).error shouldBe null
        resolveStack()
        getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        val cards = listOf("Holy Strength", "Bonesplitter", "Konda's Banner", "Erosion")
            .map { findCardsInGraveyard(1, it).single() }
        selectTargets(cards).error shouldBe null
    }

    init {
        context("Mantle of the Ancients") {

            test("returns the Auras and Equipment the creature can carry; the rest stay in the graveyard") {
                val game = board()
                game.castMantleTargetingEverything()
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.attachedTo("Holy Strength") shouldBe bears
                game.attachedTo("Bonesplitter") shouldBe bears
                withClue("Konda's Banner can be attached only to a legendary creature") {
                    game.isInGraveyard(1, "Konda's Banner") shouldBe true
                }
                withClue("Erosion enchants a land") {
                    game.isInGraveyard(1, "Erosion") shouldBe true
                }
                withClue("2/2 + Holy Strength +1/+2 + Bonesplitter +2/+0 + three attachments") {
                    game.state.projectedState.getPower(bears) shouldBe 8
                    game.state.projectedState.getToughness(bears) shouldBe 7
                }
            }

            test("with the Mantle gone, the creature it was last attached to is enchanted creature") {
                val game = board()
                game.castMantleTargetingEverything()

                val mantle = game.findPermanent("Mantle of the Ancients")!!
                game.state = game.zones.moveToZone(game.state, mantle, Zone.GRAVEYARD).state
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.attachedTo("Holy Strength") shouldBe bears
                game.attachedTo("Bonesplitter") shouldBe bears
                game.isInGraveyard(1, "Konda's Banner") shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
        }
    }
}
