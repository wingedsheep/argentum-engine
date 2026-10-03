package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Presence of Gond — "Enchant creature. Enchanted creature has '{T}: Create a 1/1 green Elf
 * Warrior creature token.'"
 */
class PresenceOfGondScenarioTest : ScenarioTestBase() {
    init {
        val grantedId = cardRegistry.getCard("Presence of Gond")!!.staticAbilities
            .filterIsInstance<GrantActivatedAbility>().single().ability.id

        test("the enchanted creature taps to create a 1/1 green Elf Warrior") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Presence of Gond", "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.execute(ActivateAbility(game.player1Id, bears, grantedId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            val tokens = game.findPermanents("Elf Warrior Token")
            tokens.size shouldBe 1
            val token = tokens.single()
            game.state.projectedState.getPower(token) shouldBe 1
            game.state.projectedState.getToughness(token) shouldBe 1
            game.state.projectedState.getColors(token) shouldBe setOf("GREEN")
            game.state.projectedState.getSubtypes(token) shouldBe setOf("Elf", "Warrior")
        }

        test("a summoning-sick enchanted creature can't use the granted tap ability") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withCardAttachedTo(1, "Presence of Gond", "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(ActivateAbility(game.player1Id, bears, grantedId)).error shouldNotBe null
            game.findPermanents("Elf Warrior Token").size shouldBe 0
        }
    }
}
