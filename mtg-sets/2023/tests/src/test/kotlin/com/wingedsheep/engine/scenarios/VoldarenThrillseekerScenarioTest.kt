package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.VoldarenThrillseeker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import io.kotest.matchers.shouldBe

/** Voldaren Thrillseeker — backup 2 and "{1}, Sacrifice: damage equal to its power to any target". */
class VoldarenThrillseekerScenarioTest : ScenarioTestBase() {
    private val ability = VoldarenThrillseeker.activatedAbilities.single().id
    private val grantedAbility = (
        ((VoldarenThrillseeker.triggeredAbilities.single().effect as CompositeEffect).effects[1] as GatedEffect).then
            as GrantActivatedAbilityEffect
        ).ability.id

    init {
        test("backup on another creature lets it sacrifice for damage equal to its power") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Voldaren Thrillseeker")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Voldaren Thrillseeker").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.execute(
                ActivateAbility(game.player1Id, bears, grantedAbility, listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 16
        }

        test("its own ability deals damage equal to its power") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Voldaren Thrillseeker")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val t = game.findPermanent("Voldaren Thrillseeker")!!
            game.execute(
                ActivateAbility(game.player1Id, t, ability, listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
        }
    }
}
