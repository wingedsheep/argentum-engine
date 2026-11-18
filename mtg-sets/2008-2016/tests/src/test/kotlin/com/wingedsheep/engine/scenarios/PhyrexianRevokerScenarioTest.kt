package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian Revoker (MBS #122) — {2} Artifact Creature — Phyrexian Horror, 2/1.
 *
 *   As this creature enters, choose a nonland card name.
 *   Activated abilities of sources with the chosen name can't be activated.
 *
 * Unlike Pithing Needle there is no mana-ability exemption: a named Llanowar Elves can't tap for
 * mana by hand, and auto-pay won't tap it either.
 */
class PhyrexianRevokerScenarioTest : ScenarioTestBase() {

    private fun TestGame.castRevokerNaming(name: String) {
        castSpell(1, "Phyrexian Revoker")
        if (hasPendingDecision() && getPendingDecision() !is ChooseOptionDecision) submitManaSourcesAutoPay()
        resolveStack()
        val decision = getPendingDecision() as ChooseOptionDecision
        withClue("the nonland pool offers nonland names only") {
            decision.options shouldContain name
            decision.options shouldNotContain "Forest"
        }
        submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name)))
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Phyrexian Revoker")
        .withCardInHand(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
        .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
        .withLandsOnBattlefield(1, "Plains", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Phyrexian Revoker locks mana abilities too") {
            test("a named Llanowar Elves can't be tapped for mana by hand") {
                val game = board()
                // Pay the Revoker with two Plains so the Elves stay untapped.
                game.castRevokerNaming("Llanowar Elves")
                val elves = game.findPermanent("Llanowar Elves")!!
                val manaAbility = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first()

                withClue("the Elves' mana ability is no longer offered") {
                    game.getLegalActions(1).none { (it.action as? ActivateAbility)?.sourceId == elves } shouldBe true
                }
                game.execute(ActivateAbility(game.player1Id, elves, manaAbility.id)).error shouldNotBe null
            }

            test("auto-pay won't tap a named mana creature") {
                val game = board()
                game.castRevokerNaming("Llanowar Elves")
                val elves = game.findPermanent("Llanowar Elves")!!
                withClue("the Revoker cost two of the three Plains, leaving the Elves untapped") {
                    game.state.getEntity(elves)!!.has<TappedComponent>() shouldBe false
                }

                // One Plains + the Elves would pay Grizzly Bears' {1}{G} without the lock.
                val bears = game.getLegalActions(1).firstOrNull { it.action is CastSpell }
                (bears?.isAffordable ?: false) shouldBe false
                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
                game.state.getEntity(elves)!!.has<TappedComponent>() shouldBe false
            }

            test("naming a different card leaves the Elves' mana ability alone and locks the named one") {
                val game = board()
                game.castRevokerNaming("Prodigal Sorcerer")
                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!

                withClue("Prodigal Sorcerer's ping is locked") {
                    game.getLegalActions(1).none { (it.action as? ActivateAbility)?.sourceId == sorcerer } shouldBe true
                }
                withClue("the unnamed Elves still pay for Grizzly Bears with the last Plains") {
                    game.castSpell(1, "Grizzly Bears").error shouldBe null
                }
            }
        }
    }
}
