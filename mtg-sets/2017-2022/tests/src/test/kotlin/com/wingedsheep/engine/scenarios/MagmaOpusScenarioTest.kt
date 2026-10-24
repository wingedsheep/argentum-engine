package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Magma Opus (STX #203) — "deals 4 damage divided as you choose among any number of targets. Tap
 * two target permanents. Create a 4/4 blue and red Elemental creature token. Draw two cards." The
 * 4 damage is divided as the spell is cast among the first group of targets only, never the two
 * permanents it taps. From hand, "{U/R}{U/R}, Discard this card: Create a Treasure token."
 */
class MagmaOpusScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Magma Opus")
        .withLandsOnBattlefield(1, "Island", 4)
        .withLandsOnBattlefield(1, "Mountain", 4)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Glory Seeker")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.opus(): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Magma Opus" }

    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true
    private fun TestGame.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        context("Magma Opus") {

            test("divides 4 among its damage targets, taps two others, makes an Elemental and draws two") {
                val game = game()
                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val seeker = game.findPermanent("Glory Seeker").shouldNotBeNull()

                val offer = game.getLegalActions(1).first { it.description == "Cast Magma Opus" }
                offer.totalDamageToDistribute shouldBe 4
                offer.distributionTargetGroup shouldBe 0

                val handBefore = game.handSize(1)
                val cast = game.execute(
                    CastSpell(
                        game.player1Id, game.opus(),
                        targets = listOf(
                            ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(giant),
                            ChosenTarget.Permanent(bears), ChosenTarget.Permanent(seeker),
                        ),
                        targetGroupCounts = listOf(2, 2),
                        damageDistribution = mapOf(game.player2Id to 2, giant to 2),
                    )
                )
                withClue("cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.damage(giant) shouldBe 2
                game.damage(bears) shouldBe 0
                game.tapped(bears) shouldBe true
                game.tapped(seeker) shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.findPermanents("Elemental Token").size shouldBe 1
                // Opus left the hand; two cards were drawn.
                game.handSize(1) shouldBe handBefore - 1 + 2
            }

            test("from hand, {U/R}{U/R} and discarding it creates a Treasure") {
                val game = game()
                val opus = game.opus()
                val action = game.getLegalActions(1)
                    .mapNotNull { it.action as? ActivateAbility }
                    .first { it.sourceId == opus }
                withClue("activate") { game.execute(action).error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(1, "Magma Opus") shouldBe true
                game.findPermanents("Treasure").size shouldBe 1
            }
        }
    }
}
