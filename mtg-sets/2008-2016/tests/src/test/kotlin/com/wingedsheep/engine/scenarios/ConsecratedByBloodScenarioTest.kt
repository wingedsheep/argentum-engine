package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.ConsecratedByBlood
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Consecrated by Blood (ORI #87) — "Enchanted creature gets +2/+2 and has flying and 'Sacrifice two
 * other creatures: Regenerate this creature.'"
 *
 * The part worth proving is the granted cost's "other": the ability's source is the enchanted
 * creature, so `notSourceItself()` must exclude the host (not the Aura) from the sacrifice pool, and
 * the regeneration shield must land on the host.
 */
class ConsecratedByBloodScenarioTest : ScenarioTestBase() {

    private val regenerateAbility = ConsecratedByBlood.staticAbilities
        .filterIsInstance<GrantActivatedAbility>()
        .single()
        .ability.id

    init {
        context("Consecrated by Blood") {

            test("the host gets +2/+2 and flying, and sacrificing two other creatures regenerates it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Consecrated by Blood", "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanents("Grizzly Bears")

                game.state.projectedState.getPower(giant) shouldBe 5
                game.state.projectedState.getToughness(giant) shouldBe 5
                game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe true

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = giant,
                        abilityId = regenerateAbility,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = bears),
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                game.findPermanents("Grizzly Bears").size shouldBe 0
                game.resolveStack()

                game.castSpell(1, "Doom Blade", giant).error shouldBe null
                game.resolveStack()

                withClue("the shield replaces the destruction — the enchanted creature survives, tapped") {
                    game.findPermanent("Hill Giant") shouldNotBe null
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                }
            }

            test("the enchanted creature can't be one of the two creatures it sacrifices") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Consecrated by Blood", "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = giant,
                        abilityId = regenerateAbility,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bear, giant)),
                    )
                )
                withClue("only one other creature, so the cost can't be paid") { activation.error shouldNotBe null }
                game.findPermanent("Hill Giant") shouldBe giant
                game.findPermanent("Grizzly Bears") shouldBe bear
            }
        }
    }
}
