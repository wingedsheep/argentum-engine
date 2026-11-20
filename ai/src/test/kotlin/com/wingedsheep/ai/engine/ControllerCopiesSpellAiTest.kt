package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ths.cards.MeletisCharlatan
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "The controller of target instant or sorcery spell copies it" — `CopyTargetSpell(copier =
 * Player.ControllerOf(...))`, Meletis Charlatan. The copy goes to the spell's own controller, so
 * the slot is a gift to that side: the AI copies its own spells and never doubles an opponent's.
 */
class ControllerCopiesSpellAiTest : ScenarioTestBase() {

    init {
        test("the spell's controller gets the copy, so the slot reads as beneficial") {
            val ability = MeletisCharlatan.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(MeletisCharlatan, ability) shouldBe listOf(TargetPolarity.BENEFICIAL)
            val reading = AiReadingAudit.read(MeletisCharlatan)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("the AI doesn't hand the opponent a second Lightning Bolt") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Meletis Charlatan")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.passPriority().error shouldBe null

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $action") {
                (action is ActivateAbility && action.sourceId == game.findPermanent("Meletis Charlatan")) shouldBe false
            }
        }
    }
}
