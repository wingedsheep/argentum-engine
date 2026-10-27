package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.RunedCrown
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Runed Crown (KHM #245) — "When this Equipment enters, you may search your library, hand, and/or
 * graveyard for a Rune card and put it onto the battlefield attached to this Equipment. If you
 * search your library this way, shuffle. Equipped creature gets +1/+1. Equip {2}"
 *
 * Proves the three-zone pool offers only Rune cards, that the chosen Rune enters attached to the
 * Crown (an Aura enchanting an Equipment), and that the Rune's Equipment grant then reaches the
 * creature the Crown equips.
 */
class RunedCrownScenarioTest : ScenarioTestBase() {

    private val equip = RunedCrown.activatedAbilities.first { it.isEquipAbility }

    private fun board() = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Runed Crown")
        .withCardInHand(1, "Rune of Speed")
        .withCardInHand(1, "Holy Strength")
        .withCardInLibrary(1, "Rune of Might")
        .withCardInGraveyard(1, "Rune of Flight")
        .apply { repeat(4) { withCardInLibrary(1, "Forest") } }
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Runed Crown") {

            test("offers Runes from library, hand and graveyard; the chosen Rune enters attached to the Crown") {
                val game = board().build()
                val speed = game.findCardsInHand(1, "Rune of Speed").single()
                val might = game.findCardsInLibrary(1, "Rune of Might").single()
                val flight = game.findCardsInGraveyard(1, "Rune of Flight").single()

                game.castSpell(1, "Runed Crown").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null

                val decision = game.getPendingDecision()
                decision as SelectCardsDecision
                withClue("only Rune cards are offered — not the non-Rune Aura Holy Strength") {
                    decision.options shouldContainExactlyInAnyOrder listOf(speed, might, flight)
                }
                game.selectCards(listOf(might)).error shouldBe null
                game.resolveStack()

                val crown = game.findPermanent("Runed Crown")!!
                val rune = game.findPermanent("Rune of Might")!!
                withClue("Rune of Might is attached to Runed Crown, which is itself unattached") {
                    game.state.getEntity(rune)!!.get<AttachedToComponent>()!!.targetId shouldBe crown
                    game.state.getEntity(crown)!!.get<AttachedToComponent>() shouldBe null
                }
                withClue("Rune of Might's enters trigger drew a card") {
                    game.handSize(1) shouldBe 3
                }

                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(game.player1Id, crown, equip.id, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                withClue("2/2 + the Crown's +1/+1 + the Rune's granted +1/+1 and trample") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                    game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("finding nothing still puts nothing onto the battlefield") {
                val game = board().build()
                game.castSpell(1, "Runed Crown").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Rune of Might") shouldBe false
                game.findCardsInLibrary(1, "Rune of Might").size shouldBe 1
            }

            test("a Rune from the graveyard enters attached to the Crown") {
                val game = board().build()
                val flight = game.findCardsInGraveyard(1, "Rune of Flight").single()

                game.castSpell(1, "Runed Crown").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.selectCards(listOf(flight)).error shouldBe null
                game.resolveStack()

                val crown = game.findPermanent("Runed Crown")!!
                val rune = game.findPermanent("Rune of Flight")!!
                game.state.getEntity(rune)!!.get<AttachedToComponent>()!!.targetId shouldBe crown
                game.isInGraveyard(1, "Rune of Flight") shouldBe false
            }

            test("declining searches nothing: no Rune enters and the library isn't shuffled") {
                val game = board().build()
                val libraryBefore = game.state.getLibrary(game.player1Id)
                game.castSpell(1, "Runed Crown").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("the library keeps its order") {
                    game.state.getLibrary(game.player1Id) shouldBe libraryBefore
                }

                game.isOnBattlefield("Rune of Speed") shouldBe false
                game.isOnBattlefield("Rune of Might") shouldBe false
                game.isOnBattlefield("Rune of Flight") shouldBe false
                game.isInHand(1, "Rune of Speed") shouldBe true
                game.isInGraveyard(1, "Rune of Flight") shouldBe true
            }
        }
    }
}
