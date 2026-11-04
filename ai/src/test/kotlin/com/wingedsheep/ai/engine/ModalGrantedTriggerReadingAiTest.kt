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
 * A granted triggered ability whose effect is modal ([com.wingedsheep.sdk.scripting.effects.ModalEffect]):
 * the trigger's controller chooses the mode, so the trigger does to its holder what the modes do,
 * when they agree. Hunter's Axe's "whenever this creature attacks, it gains your choice of trample
 * or deathtouch" is a gift either way, so equip's slot is [TargetPolarity.BENEFICIAL].
 *
 * Unread, the modal trigger vetoed the +2/+0 and the slot fell back to the removal ranking, which
 * orders our own creatures worst first: the Axe went on a 1/1 Llanowar Elves rather than the Serra
 * Angel it turns into lethal in the air.
 */
class ModalGrantedTriggerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.ours(name: String): EntityId =
        state.getBattlefield(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("a granted modal trigger whose modes all help its holder reads as a gift") {
            val card = cardRegistry.getCard("Hunter's Axe")!!
            val equip = card.script.activatedAbilities.single()
            withClue("Hunter's Axe: trample or deathtouch, the equipped creature's controller's choice") {
                intents.abilityTargetPolarities(card.name, equip.id) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("hangs Hunter's Axe on Serra Angel for lethal in the air, not on Llanowar Elves") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLifeTotal(2, 6)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(1, "Hunter's Axe")
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val equip = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == game.ours("Hunter's Axe") }

            val filled = TargetSelection.fillHeuristically(
                game.state, equip, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility

            withClue("equipped with the Axe, the 6/4 flier swings for exactly 6 over the Hill Giant") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe game.ours("Serra Angel")
            }
        }
    }
}
