package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Mirrormind Crown (ECL #258, {4}, Artifact — Equipment).
 *
 *   As long as this Equipment is attached to a creature, the first time you would
 *   create one or more tokens each turn, you may instead create that many tokens
 *   that are copies of equipped creature.
 *   Equip {2}
 *
 * Regression guard for the original consumer of
 * [com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy] — Moonlit Meditation
 * shares the same primitive, but Mirrormind Crown was the first card to use it and the
 * `attachmentVerb = "equipped"` path is exercised nowhere else.
 */
class MirrormindCrownScenarioTest : ScenarioTestBase() {

    init {
        context("Mirrormind Crown") {

            test("yes — replaces the Centaur token with a copy of the equipped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mirrormind Crown")
                    .withCardOnBattlefield(1, "Centaur Glade")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Mirrormind Crown")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val crownDef = cardRegistry.getCard("Mirrormind Crown")!!
                val equipAbility = crownDef.script.activatedAbilities.first()

                val equipResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating equip should succeed: ${equipResult.error}") {
                    equipResult.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Mirrormind Crown should be attached to Grizzly Bears") {
                    game.state.getEntity(crown)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }

                val glade = game.findPermanent("Centaur Glade")!!
                val gladeAbility = cardRegistry.getCard("Centaur Glade")!!.script.activatedAbilities.first()
                val activate = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = glade, abilityId = gladeAbility.id)
                )
                withClue("Activating Centaur Glade should succeed: ${activate.error}") {
                    activate.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("Token creation should pause for a YesNo decision; got $decision") {
                    decision.shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("No Centaur token should be created when replacement is taken") {
                    game.findPermanents("Centaur Token").size shouldBe 0
                }
                withClue("A token copy of Grizzly Bears should join the original (1 + 1 copy)") {
                    game.findPermanents("Grizzly Bears").size shouldBe 2
                }
            }

            test("no — declining leaves the original Centaur token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mirrormind Crown")
                    .withCardOnBattlefield(1, "Centaur Glade")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Mirrormind Crown")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val equipAbility = cardRegistry.getCard("Mirrormind Crown")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = crown,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val glade = game.findPermanent("Centaur Glade")!!
                val gladeAbility = cardRegistry.getCard("Centaur Glade")!!.script.activatedAbilities.first()
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = glade, abilityId = gladeAbility.id)
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("A normal Centaur token should be created when replacement is declined") {
                    game.findPermanents("Centaur Token").size shouldBe 1
                }
                withClue("Grizzly Bears count is unchanged") {
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
            }
        }
    }
}
