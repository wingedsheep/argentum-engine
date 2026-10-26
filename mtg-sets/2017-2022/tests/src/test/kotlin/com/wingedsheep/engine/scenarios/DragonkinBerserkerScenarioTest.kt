package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.DragonkinBerserker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dragonkin Berserker (KHM #131) — "Boast abilities you activate cost {1} less to activate for each
 * Dragon you control. Boast — {4}{R}: Create a 5/5 red Dragon creature token with flying."
 *
 * Each board gets exactly the mana a (reduced or unreduced) cost needs, so affordability is the
 * assertion:
 *  - the boast costs {4}{R} with no Dragons and {1} less per Dragon,
 *  - only generic mana is reduced (CR 118.7 — ruling), so {R} always remains,
 *  - another creature's boast is reduced too ("boast abilities you activate"), while that
 *    creature's ordinary activated ability is not.
 */
class DragonkinBerserkerScenarioTest : ScenarioTestBase() {

    private val boastId = DragonkinBerserker.activatedAbilities.single { it.isBoast }.id

    init {
        cardRegistry.register(
            card("Dragonkin Test Drake") {
                manaCost = "{1}"
                typeLine = "Creature — Dragon"
                power = 1
                toughness = 1
            }
        )
        cardRegistry.register(
            card("Dragonkin Test Skald") {
                manaCost = "{1}"
                typeLine = "Creature — Human Berserker"
                power = 1
                toughness = 1
                activatedAbility {
                    isBoast = true
                    cost = Costs.Mana("{2}")
                    effect = Effects.GainLife(1)
                }
                activatedAbility {
                    cost = Costs.Mana("{2}")
                    effect = Effects.GainLife(2)
                    timing = TimingRule.InstantSpeed
                }
            }
        )

        fun TestGame.canActivate(source: EntityId, abilityId: AbilityId): Boolean =
            getLegalActions(1).any { info ->
                info.isAffordable &&
                    (info.action as? ActivateAbility)?.let { it.sourceId == source && it.abilityId == abilityId } == true
            }

        /** Dragonkin Berserker (attacking), [drakes] Dragons and exactly [mountains] Mountains. */
        fun attackingBoard(drakes: Int, mountains: Int, attacker: String = "Dragonkin Berserker"): TestGame {
            var builder = scenario().withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Dragonkin Berserker")
            if (attacker != "Dragonkin Berserker") builder = builder.withCardOnBattlefield(1, attacker)
            repeat(drakes) { builder = builder.withCardOnBattlefield(1, "Dragonkin Test Drake") }
            if (mountains > 0) builder = builder.withLandsOnBattlefield(1, "Mountain", mountains)
            val game = builder.withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf(attacker to 2)).error shouldBe null
            return game
        }

        test("with no Dragons the boast costs the full {4}{R}, and makes a 5/5 flying Dragon") {
            attackingBoard(drakes = 0, mountains = 4).let { g ->
                withClue("four mana can't pay {4}{R}") {
                    g.canActivate(g.findPermanent("Dragonkin Berserker")!!, boastId) shouldBe false
                }
            }
            val game = attackingBoard(drakes = 0, mountains = 5)
            val berserker = game.findPermanent("Dragonkin Berserker")!!
            game.canActivate(berserker, boastId) shouldBe true
            game.execute(ActivateAbility(game.player1Id, berserker, boastId)).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val dragons = game.findPermanents("Dragon Token")
            dragons.size shouldBe 1
            game.state.projectedState.getPower(dragons.single()) shouldBe 5
            game.state.projectedState.getToughness(dragons.single()) shouldBe 5
        }

        test("each Dragon you control takes {1} off the boast") {
            attackingBoard(drakes = 2, mountains = 3).let { g ->
                withClue("{4}{R} less {2} is payable with three Mountains") {
                    g.canActivate(g.findPermanent("Dragonkin Berserker")!!, boastId) shouldBe true
                }
            }
            attackingBoard(drakes = 2, mountains = 3).let { g ->
                withClue("and it is really paid at that price") {
                    g.execute(ActivateAbility(g.player1Id, g.findPermanent("Dragonkin Berserker")!!, boastId)).error shouldBe null
                    if (g.hasPendingDecision()) g.submitManaSourcesAutoPay()
                    g.resolveStack()
                    g.findPermanents("Dragon Token").size shouldBe 1
                }
            }
            attackingBoard(drakes = 2, mountains = 2).let { g ->
                withClue("but not with two") {
                    g.canActivate(g.findPermanent("Dragonkin Berserker")!!, boastId) shouldBe false
                }
            }
        }

        test("only generic mana is reduced — the {R} remains however many Dragons you control") {
            attackingBoard(drakes = 6, mountains = 1).let { g ->
                g.canActivate(g.findPermanent("Dragonkin Berserker")!!, boastId) shouldBe true
            }
            attackingBoard(drakes = 6, mountains = 0).let { g ->
                g.canActivate(g.findPermanent("Dragonkin Berserker")!!, boastId) shouldBe false
            }
        }

        test("another creature's boast is reduced too, but not its ordinary activated ability") {
            val skaldDef = cardRegistry.getCard("Dragonkin Test Skald")!!
            val skaldBoast = skaldDef.activatedAbilities.single { it.isBoast }.id
            val skaldPlain = skaldDef.activatedAbilities.single { !it.isBoast }.id

            val game = attackingBoard(drakes = 1, mountains = 1, attacker = "Dragonkin Test Skald")
            val skald = game.findPermanent("Dragonkin Test Skald")!!
            withClue("the {2} boast costs {1} with one Dragon") { game.canActivate(skald, skaldBoast) shouldBe true }
            withClue("the {2} non-boast ability stays at {2}") { game.canActivate(skald, skaldPlain) shouldBe false }
        }
    }
}
