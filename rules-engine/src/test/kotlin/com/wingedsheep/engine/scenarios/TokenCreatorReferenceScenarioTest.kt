package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine test for [EffectTarget.TokenCreator] — a token's own ability naming "the permanent that
 * created it" (Gutter Grime's Oozes). The inline vat enters with two slime counters and has
 * `{0}: put a slime counter on this, then create a 0/0 Ooze with a +1/+1 counter whose base P/T equal
 * the slime counters on the permanent that created it`, plus `{0}: return this to its owner's hand`.
 *
 * Pins: the token reads its creator live; a token created without `stampCreator` names nothing;
 * and a creator that leaves and comes back is a new object (CR 400.7), so the token no longer
 * reads it even though the new object carries slime counters.
 */
class TokenCreatorReferenceScenarioTest : ScenarioTestBase() {

    private fun vat(name: String, stamp: Boolean) = card(name) {
        manaCost = "{0}"
        colorIdentity = "G"
        typeLine = "Enchantment"
        oracleText = "Enters with two slime counters. {0}: slime + Ooze. {0}: return to hand."

        replacementEffect(EntersWithCounters(counterType = CounterType.SLIME, count = 2, selfOnly = true))

        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.AddCounters(CounterType.SLIME, 1, EffectTarget.Self) then Effects.CreateToken(
                power = 0,
                toughness = 0,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Ooze"),
                stampCreator = stamp,
                // One +1/+1 counter keeps a token alive when its creator names nothing, so the
                // read itself is what the tests observe.
                initialCounters = mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1),
                staticAbilities = listOf(
                    SetBasePowerToughnessDynamicStatic(
                        power = DynamicAmounts.countersOn(EffectTarget.TokenCreator, CounterType.SLIME),
                        toughness = DynamicAmounts.countersOn(EffectTarget.TokenCreator, CounterType.SLIME),
                        filter = GroupFilter.source(),
                    )
                ),
            )
        }

        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.ReturnToHand(EffectTarget.Self)
        }
    }

    init {
        cardRegistry.register(vat("Test Slime Vat", stamp = true))
        cardRegistry.register(vat("Test Unstamped Vat", stamp = false))

        fun game(name: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
            .also { g ->
                g.castSpell(1, name).error shouldBe null
                g.resolveStack()
            }

        fun TestGame.activate(name: String, index: Int) {
            val id = findPermanent(name)!!
            val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
            execute(ActivateAbility(playerId = player1Id, sourceId = id, abilityId = ability.id)).error shouldBe null
            resolveStack()
        }

        fun TestGame.oozes(): List<EntityId> = findPermanents("Ooze Token")

        fun TestGame.pt(id: EntityId) = state.projectedState.getPower(id) to state.projectedState.getToughness(id)

        test("the token reads the counters on the permanent that created it, live") {
            val g = game("Test Slime Vat")
            g.activate("Test Slime Vat", 0)
            withClue("two entering slime counters + one, plus its own +1/+1 counter") {
                g.pt(g.oozes().single()) shouldBe (4 to 4)
            }
            g.activate("Test Slime Vat", 0)
            g.oozes() shouldHaveSize 2
            g.oozes().forEach { g.pt(it) shouldBe (5 to 5) }
        }

        test("without stampCreator the token has no creator to read") {
            val g = game("Test Unstamped Vat")
            g.activate("Test Unstamped Vat", 0)
            g.pt(g.oozes().single()) shouldBe (1 to 1)
        }

        test("a creator that leaves and comes back is a new object: the token no longer reads it") {
            val g = game("Test Slime Vat")
            g.activate("Test Slime Vat", 0)
            val first = g.oozes().single()
            g.pt(first) shouldBe (4 to 4)
            g.activate("Test Slime Vat", 1)
            withClue("its creator left the battlefield: base 0/0, only its +1/+1 counter left") {
                g.pt(first) shouldBe (1 to 1)
            }
            g.castSpell(1, "Test Slime Vat").error shouldBe null
            g.resolveStack()
            withClue("the returned vat (two slime counters) is a new object the old Ooze doesn't read") {
                g.pt(first) shouldBe (1 to 1)
            }
            g.activate("Test Slime Vat", 0)
            val second = g.oozes().single { it != first }
            withClue("the new object's own Ooze reads it") { g.pt(second) shouldBe (4 to 4) }
            g.pt(first) shouldBe (1 to 1)
        }
    }
}
