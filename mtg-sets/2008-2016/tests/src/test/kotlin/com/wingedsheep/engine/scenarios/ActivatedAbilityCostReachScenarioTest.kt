package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `ReduceActivatedAbilityCost.reach` — the default `MATCHING_PERMANENTS` reach covers abilities of
 * permanents on the battlefield only. Training Grounds' ruling: "Training Grounds affects only
 * creatures you control on the battlefield. The costs of activated abilities that work in other
 * zones (such as cycling or unearth) won't be reduced."
 *
 * The enumerator never priced a graveyard ability through the board's statics, but the activation
 * handler did — so a creature card's scavenge was offered at full price and then charged at the
 * reduced one. Both sides now agree: a graveyard card is not a permanent.
 */
class ActivatedAbilityCostReachScenarioTest : ScenarioTestBase() {

    init {
        test("Training Grounds does not reduce a creature card's scavenge from the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Training Grounds")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Drudge Beetle")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val beetle = game.findCardsInGraveyard(1, "Drudge Beetle").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("four Forests can't pay the printed {5}{G}, so scavenge isn't offered") {
                game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == beetle } shouldBe false
            }

            val abilityId = cardRegistry.getCard("Drudge Beetle")!!.activatedAbilities.first().id
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id, sourceId = beetle, abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            )
            withClue("the handler charges the printed cost too — a {3}{G} would have been paid") {
                result.error shouldNotBe null
            }
        }
    }
}
