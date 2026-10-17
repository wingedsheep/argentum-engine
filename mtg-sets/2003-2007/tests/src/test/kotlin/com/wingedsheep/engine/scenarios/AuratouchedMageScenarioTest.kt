package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Auratouched Mage (RAV #1) — "When this creature enters, search your library for an Aura card
 * that could enchant it. If this creature is still on the battlefield, put that Aura card onto the
 * battlefield attached to it. Otherwise, reveal the Aura card and put it into your hand. Then
 * shuffle."
 *
 * Pins the two new pieces: `CardPredicate.CouldAttachTo` narrows the search to Auras whose printed
 * enchant restriction the Mage satisfies (Holy Strength yes, Erosion's "enchant land" no), and
 * `MoveCollectionEffect.attachTo` puts the found Aura onto the battlefield attached to the Mage
 * with no enchant choice. With the Mage gone, the Aura goes to hand instead.
 */
class AuratouchedMageScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Auratouched Mage")
        .withLandsOnBattlefield(1, "Plains", 6)
        .withCardInLibrary(1, "Holy Strength")
        .withCardInLibrary(1, "Erosion")
        .withCardInLibrary(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Auratouched Mage") {

            test("finds only an Aura that could enchant it and puts it onto the battlefield attached to it") {
                val game = board().build()
                val holyStrength = game.findCardsInLibrary(1, "Holy Strength").single()

                game.castSpell(1, "Auratouched Mage").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Erosion (enchant land) and the non-Aura can't be found") {
                    decision.options shouldBe listOf(holyStrength)
                }
                game.selectCards(listOf(holyStrength)).error shouldBe null
                game.resolveStack()

                val mage = game.findPermanent("Auratouched Mage")!!
                withClue("Holy Strength entered attached to the Mage") {
                    game.isOnBattlefield("Holy Strength") shouldBe true
                    val aura = game.findPermanent("Holy Strength")!!
                    game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe mage
                    game.state.projectedState.getPower(mage) shouldBe 4
                    game.state.projectedState.getToughness(mage) shouldBe 5
                }
                withClue("the rest of the library stays put") {
                    game.librarySize(1) shouldBe 2
                    game.handSize(1) shouldBe 0
                }
            }

            test("an Aura of a color the Mage has protection from could not enchant it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Auratouched Mage")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "White Ward", "Grizzly Bears")
                    .withCardInLibrary(1, "Holy Strength")
                    .withCardInLibrary(1, "Unholy Strength")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val unholy = game.findCardsInLibrary(1, "Unholy Strength").single()

                game.castSpell(1, "Auratouched Mage").error shouldBe null
                game.passPriority()
                game.passPriority()
                val mage = game.findPermanent("Auratouched Mage")!!
                withClue("the enters trigger is waiting on the stack") { game.state.stack.size shouldBe 1 }

                // Move White Ward ("enchanted creature has protection from white") onto the Mage.
                val ward = game.findPermanent("White Ward")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state
                    .updateEntity(ward) { it.with(AttachedToComponent(mage)) }
                    .updateEntity(bears) { it.without<AttachmentsComponent>() }
                    .updateEntity(mage) { it.with(AttachmentsComponent(listOf(ward))) }

                game.resolveStack()
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("white Holy Strength couldn't enchant a pro-white Mage (CR 702.16c)") {
                    decision.options shouldBe listOf(unholy)
                }
                game.selectCards(listOf(unholy)).error shouldBe null
                game.resolveStack()
                val aura = game.findPermanent("Unholy Strength")!!
                game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe mage
            }

            test("finding nothing is legal") {
                val game = board().build()
                game.castSpell(1, "Auratouched Mage").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Holy Strength") shouldBe false
                game.librarySize(1) shouldBe 3
            }

            test("if the Mage has left the battlefield, the Aura is revealed and put into hand") {
                val game = board().build()
                val holyStrength = game.findCardsInLibrary(1, "Holy Strength").single()

                game.castSpell(1, "Auratouched Mage").error shouldBe null
                // Resolve the creature spell only; its enters trigger goes on the stack.
                game.passPriority()
                game.passPriority()
                val mage = game.findPermanent("Auratouched Mage")!!
                withClue("the enters trigger is waiting on the stack") { game.state.stack.size shouldBe 1 }

                // The Mage leaves in response to its trigger.
                game.state = zones.moveToZone(game.state, mage, Zone.GRAVEYARD).state
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldBe listOf(holyStrength)
                game.selectCards(listOf(holyStrength)).error shouldBe null
                game.resolveStack()

                withClue("no host, so the Aura goes to hand") {
                    game.isInHand(1, "Holy Strength") shouldBe true
                    game.isOnBattlefield("Holy Strength") shouldBe false
                }
            }
        }
    }
}
