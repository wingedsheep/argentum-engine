package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * League Guildmage (GRN #185) — {U}{R} 2/2 Human Wizard.
 *
 *   {3}{U}, {T}: Draw a card.
 *   {X}{R}, {T}: Copy target instant or sorcery spell you control with mana value X.
 *   You may choose new targets for the copy.
 *
 * The X chosen for the activation decides which spells are legal targets.
 */
class LeagueGuildmageScenarioTest : ScenarioTestBase() {
    private fun TestGame.copy(spell: EntityId, x: Int) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("League Guildmage")!!,
                abilityId = cardRegistry.getCard("League Guildmage")!!.script.activatedAbilities[1].id,
                targets = listOf(ChosenTarget.Spell(spell)),
                xValue = x,
            )
        )

    private fun TestGame.castBolt(): EntityId {
        castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
        return state.stack.last()
    }

    init {
        test("X equal to the spell's mana value copies it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "League Guildmage")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.castBolt()
            game.copy(bolt, x = 1).error shouldBe null
            game.resolveStack()

            (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 14
        }

        test("X different from the spell's mana value can't target it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "League Guildmage")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bolt = game.castBolt()
            game.copy(bolt, x = 2).error shouldNotBe null
        }
    }
}
