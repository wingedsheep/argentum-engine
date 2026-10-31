package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The target polarity of an equip ([com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect]):
 * the slot does to the creature what the Equipment does to the creature it is attached to — its
 * "equipped creature …" statics and triggers, read the way an Aura's are.
 *
 * Unread, the slot kept the removal ranking, which puts our *worst* creature first; with more
 * creatures than the simulation's candidate cut, the creature the Equipment belongs on was never
 * even tried.
 */
class AttachEquipmentReadingAiTest : ScenarioTestBase() {

    init {
        test("an equip slot reads as the Equipment's gift to its host") {
            val bonesplitter = cardRegistry.getCard("Bonesplitter")!!
            val equip = bonesplitter.script.activatedAbilities.single()
            withClue("Bonesplitter: equipped creature gets +2/+0") {
                TargetPolarityAnalyzer.forAbility(bonesplitter, equip) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("equips the flier that turns the Equipment into lethal, not one of twenty-two ground 1/1s") {
            var builder = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLifeTotal(2, 4)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Wind Drake")
            repeat(22) {
                builder = builder.withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Grizzly Bears")
            }
            val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val target = (action as? ActivateAbility)?.targets?.singleOrNull() as? ChosenTarget.Permanent
            withClue(
                "chose $action — an equip slot reads beneficial, so Wind Drake (2 in the air, 4 with " +
                    "Bonesplitter against 4 life) is ranked first rather than cut behind twenty-two goblins",
            ) {
                action.shouldBeInstanceOf<ActivateAbility>()
                target?.let { game.state.getEntity(it.entityId)?.get<CardComponent>()?.name } shouldBe "Wind Drake"
            }
        }
    }
}
