package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Corrupted Shapeshifter (MH3 #56) — {3}{U} Creature — Eldrazi Shapeshifter, star/star.
 * "Devoid. As this creature enters, it becomes your choice of a 3/3 creature with flying, a 2/5
 * creature with vigilance, or a 0/12 creature with defender."
 */
class CorruptedShapeshifterScenarioTest : ScenarioTestBase() {

    init {
        fun castAndChoose(label: String): Pair<TestGame, EntityId> {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Corrupted Shapeshifter")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Corrupted Shapeshifter").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(label)))
            game.resolveStack()
            return game to game.findPermanent("Corrupted Shapeshifter")!!
        }

        context("Corrupted Shapeshifter") {
            test("3/3 with flying") {
                val (game, id) = castAndChoose("3/3 creature with flying")
                val projected = game.state.projectedState
                projected.getPower(id) shouldBe 3
                projected.getToughness(id) shouldBe 3
                projected.hasKeyword(id, Keyword.FLYING) shouldBe true
                projected.hasKeyword(id, Keyword.VIGILANCE) shouldBe false
                withClue("devoid: colorless") { projected.getColors(id).isEmpty() shouldBe true }
            }

            test("2/5 with vigilance") {
                val (game, id) = castAndChoose("2/5 creature with vigilance")
                val projected = game.state.projectedState
                projected.getPower(id) shouldBe 2
                projected.getToughness(id) shouldBe 5
                projected.hasKeyword(id, Keyword.VIGILANCE) shouldBe true
                projected.hasKeyword(id, Keyword.FLYING) shouldBe false
            }

            test("0/12 with defender, and the chosen shape is its copiable card") {
                val (game, id) = castAndChoose("0/12 creature with defender")
                val projected = game.state.projectedState
                projected.getPower(id) shouldBe 0
                projected.getToughness(id) shouldBe 12
                projected.hasKeyword(id, Keyword.DEFENDER) shouldBe true
                // Copiable values (CR 707.2): the card a copy effect copies is the chosen shape.
                val card = game.state.getEntity(id)!!.get<CardComponent>()!!
                card.baseStats?.basePower shouldBe 0
                card.baseStats?.baseToughness shouldBe 12
                card.baseKeywords.contains(Keyword.DEFENDER) shouldBe true
            }
        }
    }
}
