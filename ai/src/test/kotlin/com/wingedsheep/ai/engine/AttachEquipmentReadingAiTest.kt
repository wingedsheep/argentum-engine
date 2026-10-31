package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * What equipping does ([com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect]): "attach this
 * Equipment to target creature" does to that creature whatever the Equipment does to the creature
 * it equips — +2 Mace's "+2/+2" is a gift, so the slot is [TargetPolarity.BENEFICIAL].
 *
 * Unread, the slot fell back to the removal ranking, which orders our own creatures worst first:
 * the cheap target pick hung +2 Mace on a 1/1 Llanowar Elves rather than the Serra Angel it turns
 * into lethal in the air.
 */
class AttachEquipmentReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.ours(name: String): EntityId =
        state.getBattlefield(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("equip's target reads as the Equipment's gift to the creature it equips") {
            val mace = cardRegistry.getCard("+2 Mace")!!
            val equip = mace.script.activatedAbilities.single()
            withClue("+2 Mace: equipped creature gets +2/+2, so equip's target is ours to pick") {
                intents.abilityTargetPolarities(mace.name, equip.id) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            val embercleave = cardRegistry.getCard("Embercleave")!!
            val attach = embercleave.script.triggeredAbilities.single { it.targetRequirement != null }
            withClue("Embercleave's 'attach it to target creature you control' trigger, read off its source") {
                intents.effectTargetPolarities(attach.effect, listOf(attach.targetRequirement!!), embercleave.name) shouldBe
                    listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("hangs +2 Mace on Serra Angel for lethal in the air, not on Llanowar Elves") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLifeTotal(2, 6)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "+2 Mace")
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val equip = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == game.ours("+2 Mace") }

            val filled = TargetSelection.fillHeuristically(
                game.state, equip, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility

            withClue("equipped with +2 Mace, the 4/4 flier swings for exactly 6 over the Hill Giant") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.ours("Serra Angel")
            }
        }
    }
}
