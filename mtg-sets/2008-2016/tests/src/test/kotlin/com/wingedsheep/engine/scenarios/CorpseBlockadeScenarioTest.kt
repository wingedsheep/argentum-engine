package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Corpse Blockade (GTC) — Defender; sacrifice another creature: gains deathtouch until end of turn. */
class CorpseBlockadeScenarioTest : ScenarioTestBase() {
    private val abilityId get() = cardRegistry.getCard("Corpse Blockade")!!.script.activatedAbilities.single().id

    init {
        test("sacrificing another creature grants deathtouch") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Corpse Blockade")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val blockade = game.findPermanent("Corpse Blockade")!!
            game.state.projectedState.hasKeyword(blockade, Keyword.DEFENDER) shouldBe true
            game.state.projectedState.hasKeyword(blockade, Keyword.DEATHTOUCH) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, blockade, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            withClue("Grizzly Bears is the only other creature, so it is sacrificed") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            game.resolveStack()

            game.state.projectedState.hasKeyword(blockade, Keyword.DEATHTOUCH) shouldBe true
        }

        test("it can't sacrifice itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Corpse Blockade")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val blockade = game.findPermanent("Corpse Blockade")!!

            game.execute(ActivateAbility(game.player1Id, blockade, abilityId)).error shouldNotBe null
            game.isOnBattlefield("Corpse Blockade") shouldBe true
        }
    }
}
