package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Oppressive Rays (JOU #19) — {W} Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block unless its controller pays {3}.
 *   Activated abilities of enchanted creature cost {3} more to activate.
 *
 * The tax has no "unless they're mana abilities" carve-out, so an enchanted Llanowar Elves'
 * `{T}: Add {G}` costs `{3}, {T}` (CR 605.1a: a mana ability is an activated ability). The
 * enumerator offers it at that total, a manual activation charges it, and auto-pay won't tap the
 * Elves for free.
 */
class OppressiveRaysScenarioTest : ScenarioTestBase() {

    private fun elvesManaAbilityId() =
        cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.first().id

    private fun TestGame.pool() = state.getEntity(player1Id)!!.get<ManaPoolComponent>()

    private fun raysOnElves(plains: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
        .withCardAttachedTo(2, "Oppressive Rays", "Llanowar Elves")
        .withLandsOnBattlefield(1, "Plains", plains)
        .withCardInHand(1, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Oppressive Rays taxes the enchanted creature's mana ability") {
            test("the mana ability is offered at {3}, {T} and pays the tax when activated") {
                val game = raysOnElves(plains = 3)
                val elves = game.findPermanent("Llanowar Elves")!!

                val offer = game.getLegalActions(1).first { (it.action as? ActivateAbility)?.sourceId == elves }
                withClue("the offer shows the taxed total") { offer.manaCostString shouldBe "{3}" }
                withClue("three untapped Plains pay the tax") { offer.isAffordable shouldBe true }

                val result = game.execute(ActivateAbility(game.player1Id, elves, elvesManaAbilityId()))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("the Elves tapped and made {G}") {
                    game.state.getEntity(elves)!!.has<TappedComponent>() shouldBe true
                    game.pool()?.green shouldBe 1
                }
                withClue("the {3} was paid from the Plains, not floated") {
                    game.findPermanents("Plains").all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
                    game.pool()?.white shouldBe 0
                }
            }

            test("with only two lands the taxed ability can't be paid") {
                val game = raysOnElves(plains = 2)
                val elves = game.findPermanent("Llanowar Elves")!!

                val offer = game.getLegalActions(1).first { (it.action as? ActivateAbility)?.sourceId == elves }
                offer.isAffordable shouldBe false
                game.execute(ActivateAbility(game.player1Id, elves, elvesManaAbilityId())).error shouldNotBe null
                game.state.getEntity(elves)!!.has<TappedComponent>() shouldBe false
            }

            test("auto-pay won't tap the taxed Elves for free") {
                // One Plains + the Elves would pay Grizzly Bears' {1}{G} if the Elves were untaxed.
                val game = raysOnElves(plains = 1)
                val elves = game.findPermanent("Llanowar Elves")!!

                val bears = game.getLegalActions(1).firstOrNull { it.action is CastSpell }
                withClue("Grizzly Bears isn't castable: the only green source costs {3} more") {
                    (bears?.isAffordable ?: false) shouldBe false
                }
                game.castSpell(1, "Grizzly Bears").error shouldNotBe null
                game.state.getEntity(elves)!!.has<TappedComponent>() shouldBe false
            }
        }

        context("Oppressive Rays — attacking costs {3}") {
            test("declaring the enchanted creature as an attacker stops to charge the {3}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(2, "Oppressive Rays", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                withClue("the Aura's tax stopped the declaration to charge for it") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
