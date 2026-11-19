package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bng.cards.ClaimOfErebos
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Claim of Erebos (BNG #64) — "Enchanted creature has "{1}{B}, {T}: Target player loses 2 life.""
 */
class ClaimOfErebosScenarioTest : ScenarioTestBase() {

    private val drainAbilityId = ClaimOfErebos.staticAbilities
        .filterIsInstance<GrantActivatedAbility>()
        .single()
        .ability.id

    init {
        test("the enchanted creature taps and pays {1}{B} to make a player lose 2 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Claim of Erebos", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = bears,
                    abilityId = drainAbilityId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the target player lost 2 life") { game.getLifeTotal(2) shouldBe 18 }
            withClue("the enchanted creature tapped for it") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
