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
 * Equipping an Equipment that grants a triggered ability
 * ([com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect] over a
 * [com.wingedsheep.sdk.scripting.GrantTriggeredAbility] static): the equipped creature's controller
 * controls the granted trigger, so a token it creates is theirs and a target it names is theirs to
 * pick. Ceremonial Knife's "+1/+0 and 'whenever this creature deals combat damage, create a Blood
 * token'" is a gift, so equip's slot is [TargetPolarity.BENEFICIAL].
 *
 * Unread, the granted trigger vetoed the +1/+0 and the slot fell back to the removal ranking, which
 * orders our own creatures worst first: the Knife went on a 1/1 Llanowar Elves rather than the Serra
 * Angel it turns into lethal in the air.
 */
class GrantedTriggerEquipReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.ours(name: String): EntityId =
        state.getBattlefield(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("an Equipment's granted trigger that pays its controller reads as a gift") {
            for (name in listOf("Ceremonial Knife", "Spare Dagger")) {
                val card = cardRegistry.getCard(name)!!
                val equip = card.script.activatedAbilities.single()
                withClue("$name: the granted trigger is the equipped creature's controller's to use") {
                    intents.abilityTargetPolarities(card.name, equip.id) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
            }
        }

        test("hangs Ceremonial Knife on Serra Angel for lethal in the air, not on Llanowar Elves") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLifeTotal(2, 5)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Ceremonial Knife")
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val equip = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == game.ours("Ceremonial Knife") }

            val filled = TargetSelection.fillHeuristically(
                game.state, equip, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility

            withClue("equipped with the Knife, the 5/4 flier swings for exactly 5 over the Hill Giant") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.ours("Serra Angel")
            }
        }
    }
}
