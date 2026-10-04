package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gryff's Boon (SOI #20) — {W} Enchantment — Aura.
 *
 * "Enchant creature
 *  Enchanted creature gets +1/+0 and has flying.
 *  {3}{W}: Return this card from your graveyard to the battlefield attached to target creature.
 *  Activate only as a sorcery."
 *
 * The recursion ability used to move the *target creature* to the battlefield (a no-op on a
 * creature already there) and leave the Aura in the graveyard; it must return the Aura itself,
 * attached to the target.
 */
class GryffsBoonScenarioTest : ScenarioTestBase() {

    init {
        context("Gryff's Boon") {

            test("the enchanted creature gets +1/+0 and flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Gryff's Boon", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }

            test("it returns from the graveyard attached to the target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Gryff's Boon")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boon = game.findCardsInGraveyard(1, "Gryff's Boon").single()
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.requireCard("Gryff's Boon").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boon,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val returned = game.findPermanent("Gryff's Boon")
                withClue("the Aura is on the battlefield attached to the Bears") {
                    returned shouldNotBe null
                    game.state.getEntity(returned!!)?.get<AttachedToComponent>()?.targetId shouldBe bears
                    game.isInGraveyard(1, "Gryff's Boon") shouldBe false
                }
                withClue("and it is buffing its new host") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                }
            }

            test("it can enchant an opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Gryff's Boon")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val boon = game.findCardsInGraveyard(1, "Gryff's Boon").single()
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.requireCard("Gryff's Boon").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = boon,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val returned = game.findPermanent("Gryff's Boon")!!
                game.state.getEntity(returned)?.get<AttachedToComponent>()?.targetId shouldBe bears
            }
        }
    }
}
