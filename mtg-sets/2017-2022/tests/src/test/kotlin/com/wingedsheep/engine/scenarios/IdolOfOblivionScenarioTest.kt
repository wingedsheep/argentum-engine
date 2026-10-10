package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Idol of Oblivion (C19 #55).
 *
 * "{T}: Draw a card. Activate only if you created a token this turn.
 *  {8}, {T}, Sacrifice this artifact: Create a 10/10 colorless Eldrazi creature token."
 *
 * Two Idols: the first one's sacrifice ability creates the Eldrazi, which turns on the second one's
 * draw — and before that, the draw is rejected.
 */
class IdolOfOblivionScenarioTest : ScenarioTestBase() {

    private val idol by lazy { cardRegistry.getCard("Idol of Oblivion")!!.script }
    private val drawAbilityId by lazy { idol.activatedAbilities[0].id }
    private val eldraziAbilityId by lazy { idol.activatedAbilities[1].id }

    init {
        context("Idol of Oblivion") {

            test("the draw is rejected until you create a token, then works") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Idol of Oblivion")
                    .withCardOnBattlefield(1, "Idol of Oblivion")
                    .withLandsOnBattlefield(1, "Wastes", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { builder = builder.withCardInLibrary(1, "Wastes") }
                repeat(4) { builder = builder.withCardInLibrary(2, "Wastes") }
                val game = builder.build()

                val (first, second) = game.findAllPermanents("Idol of Oblivion")
                val handBefore = game.handSize(1)

                val early = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = second, abilityId = drawAbilityId)
                )
                withClue("No token created yet — the draw must be rejected") {
                    (early.error != null) shouldBe true
                    game.handSize(1) shouldBe handBefore
                }

                val sac = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = first, abilityId = eldraziAbilityId)
                )
                withClue("Sacrificing the first Idol for {8}: ${sac.error}") { sac.error shouldBe null }
                game.resolveStack()

                val eldrazi = (game.findPermanent("Eldrazi Token") ?: game.findPermanent("Eldrazi"))!!
                withClue("A 10/10 Eldrazi token was created and the first Idol is gone") {
                    game.state.getEntity(eldrazi)?.has<TokenComponent>() shouldBe true
                    game.state.projectedState.getPower(eldrazi) shouldBe 10
                    game.state.projectedState.getToughness(eldrazi) shouldBe 10
                    game.findAllPermanents("Idol of Oblivion").size shouldBe 1
                }

                val draw = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = second, abilityId = drawAbilityId)
                )
                withClue("Having created a token, the draw is legal: ${draw.error}") { draw.error shouldBe null }
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
