package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ajani's Chosen (M14 #2) — {2}{W}{W} Creature — Cat Soldier 3/3.
 *
 *   Whenever an enchantment you control enters, create a 2/2 white Cat creature token. If that
 *   enchantment is an Aura, you may attach it to the token.
 *
 * Pins the create-then-may-attach composition: the "you may" pauses mid-pipeline, so the attach
 * must still find the token the create step made; declining leaves the Aura where it was; a
 * non-Aura enchantment makes a token with no prompt.
 */
class AjanisChosenScenarioTest : ScenarioTestBase() {

    init {
        fun castHolyStrengthOnChosen(): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ajani's Chosen")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Holy Strength")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chosen = game.findPermanent("Ajani's Chosen")!!
            val cast = game.castSpell(1, "Holy Strength", chosen)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()
            return game
        }

        test("an Aura entering makes a Cat and may move onto it") {
            val game = castHolyStrengthOnChosen()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            game.resolveStack()

            val cat = game.findPermanent("Cat Token").shouldNotBeNull()
            val aura = game.findPermanent("Holy Strength")!!
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe cat
            val projected = game.state.projectedState
            projected.getPower(cat) shouldBe 3
            projected.getToughness(cat) shouldBe 4
            projected.getPower(game.findPermanent("Ajani's Chosen")!!) shouldBe 3
        }

        test("declining leaves the Aura on its original host") {
            val game = castHolyStrengthOnChosen()

            game.answerYesNo(false)
            game.resolveStack()

            game.findPermanent("Cat Token").shouldNotBeNull()
            val chosen = game.findPermanent("Ajani's Chosen")!!
            val aura = game.findPermanent("Holy Strength")!!
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe chosen
        }

        test("a non-Aura enchantment makes a Cat without asking") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ajani's Chosen")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInHand(1, "Glorious Anthem")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Glorious Anthem")
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.findAllPermanents("Cat Token").size shouldBe 1
        }
    }
}
