package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Twisted Sewer-Witch. */
class TwistedSewerWitchScenarioTest : ScenarioTestBase() {

    private val lecternAbilityId by lazy {
        cardRegistry.requireCard("Living Lectern").activatedAbilities[0].id
    }

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun rolesAttachedTo(game: TestGame, roleName: String, host: EntityId): Int =
        game.findAllPermanents(roleName).count { role ->
            game.state.getEntity(role)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Twisted Sewer-Witch — a Wicked Role on every Rat you control") {
            test("the Rat created by the same trigger is inside the 'for each Rat' snapshot") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twisted Sewer-Witch")
                    // A Rat already on the battlefield, so the count spans pre-existing and new.
                    .withCardOnBattlefield(1, "Voracious Vermin")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vermin = game.findPermanent("Voracious Vermin")!!

                game.castSpell(1, "Twisted Sewer-Witch").error shouldBe null
                game.resolveStack()

                val witch = game.findPermanent("Twisted Sewer-Witch")!!
                val ratTokens = game.findAllPermanents("Rat Token")

                withClue("the Witch's ETB created one Rat token") {
                    ratTokens.size shouldBe 1
                }

                withClue("every Rat you control got a Wicked Role — the new token included") {
                    rolesAttachedTo(game, "Wicked Role", ratTokens.single()) shouldBe 1
                    rolesAttachedTo(game, "Wicked Role", vermin) shouldBe 1
                    game.findAllPermanents("Wicked Role").size shouldBe 2
                }

                withClue("the Witch is a Human Warlock, not a Rat, so it gets no Role") {
                    rolesAttachedTo(game, "Wicked Role", witch) shouldBe 0
                }

                withClue("Wicked Role grants +1/+1 — the 1/1 Rat token projects as 2/2") {
                    game.state.projectedState.getPower(ratTokens.single()) shouldBe 2
                    game.state.projectedState.getToughness(ratTokens.single()) shouldBe 2
                }
            }
        }
    }
}
