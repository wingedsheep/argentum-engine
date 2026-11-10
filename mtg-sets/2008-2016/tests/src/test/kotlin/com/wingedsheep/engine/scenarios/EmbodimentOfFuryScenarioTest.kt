package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Embodiment of Fury (OGW #107).
 *
 * Pins the composition a reader can't confirm from the script alone: the landfall animate keeps the
 * land a land (with its own subtype) while adding Elemental and haste, and the static "land creatures
 * you control have trample" — a layer 6 grant whose filter depends on the layer 4 type change — picks
 * up the land only while it is a creature. Declining the "you may" leaves the land untouched.
 */
class EmbodimentOfFuryScenarioTest : ScenarioTestBase() {

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Embodiment of Fury")
        .withCardOnBattlefield(1, "Mountain")
        .withCardInHand(1, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("landfall animates target land you control into a 3/3 Elemental land creature with haste and trample") {
            val game = base().build()
            val target = game.findPermanent("Mountain")!!
            val played = game.findCardsInHand(1, "Mountain").single()

            withClue("a land that isn't a creature doesn't have trample") {
                game.state.projectedState.hasKeyword(target, Keyword.TRAMPLE) shouldBe false
            }

            game.execute(PlayLand(game.player1Id, played)).error shouldBe null
            game.selectTargets(listOf(target)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("it's still a land, and keeps its land subtype") {
                projected.isCreature(target) shouldBe true
                projected.hasType(target, "LAND") shouldBe true
                projected.hasSubtype(target, "Mountain") shouldBe true
                projected.hasSubtype(target, "Elemental") shouldBe true
            }
            projected.getPower(target) shouldBe 3
            projected.getToughness(target) shouldBe 3
            projected.hasKeyword(target, Keyword.HASTE) shouldBe true
            withClue("land creatures you control have trample") {
                projected.hasKeyword(target, Keyword.TRAMPLE) shouldBe true
            }
            withClue("the land that wasn't animated stays a plain land") {
                val other = game.findPermanents("Mountain").single { it != target }
                projected.isCreature(other) shouldBe false
            }
        }

        test("declining the may leaves the land as it was") {
            val game = base().build()
            val target = game.findPermanent("Mountain")!!
            val played = game.findCardsInHand(1, "Mountain").single()

            game.execute(PlayLand(game.player1Id, played)).error shouldBe null
            game.selectTargets(listOf(target)).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(target) shouldBe false
            projected.hasKeyword(target, Keyword.TRAMPLE) shouldBe false
        }
    }
}
