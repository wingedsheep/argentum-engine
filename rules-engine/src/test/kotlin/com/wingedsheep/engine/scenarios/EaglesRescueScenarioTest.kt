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
 * Eagle's Rescue (HOB #155) — {2}{W/U}{W/U} Enchantment — Aura.
 *
 * "Enchant creature
 *  Enchanted creature gets +2/+2 and has flying.
 *  {2}{W/U}{W/U}: Return this card from your graveyard to the battlefield attached to target
 *  creature you control with power 1 or less. Activate only as a sorcery."
 *
 * The recursion ability is activated from the **graveyard**, which is a separate enumeration path
 * from battlefield abilities, and its target is restricted to power 1 or less — a bigger creature
 * must not be offerable.
 */
class EaglesRescueScenarioTest : ScenarioTestBase() {

    init {
        context("Eagle's Rescue") {

            test("the enchanted creature gets +2/+2 and flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Eagle's Rescue", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }

            test("it returns from the graveyard attached to a small creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Eagle's Rescue")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rescue = game.findCardsInGraveyard(1, "Eagle's Rescue").single()
                val mystic = game.findPermanent("Llanowar Elves")!!
                val abilityId = cardRegistry.requireCard("Eagle's Rescue").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rescue,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(mystic)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val returned = game.findPermanent("Eagle's Rescue")
                withClue("the Aura is on the battlefield attached to the 1/1") {
                    returned shouldNotBe null
                    game.state.getEntity(returned!!)?.get<AttachedToComponent>()?.targetId shouldBe mystic
                    game.isInGraveyard(1, "Eagle's Rescue") shouldBe false
                }
                withClue("and it is buffing its new host") {
                    game.state.projectedState.getPower(mystic) shouldBe 3
                    game.state.projectedState.getToughness(mystic) shouldBe 3
                    game.state.projectedState.hasKeyword(mystic, Keyword.FLYING) shouldBe true
                }
            }

            test("a creature with power 2 is not a legal target for the recursion") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Eagle's Rescue")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rescue = game.findCardsInGraveyard(1, "Eagle's Rescue").single()
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.requireCard("Eagle's Rescue").activatedAbilities.single().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rescue,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("power 2 fails the 'power 1 or less' restriction") {
                    result.error shouldNotBe null
                    game.isInGraveyard(1, "Eagle's Rescue") shouldBe true
                }
            }
        }
    }
}
