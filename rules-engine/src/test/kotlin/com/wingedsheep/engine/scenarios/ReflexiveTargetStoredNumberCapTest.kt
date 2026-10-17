package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A reflexive trigger's target filter reads the numbers its action half stored (CR 603.12).
 *
 * The reflexive ability's targets are chosen as it goes on the stack, after the spell that fired it
 * has finished resolving, so a "mana value less than or equal to that amount" cap must come from the
 * pipeline the trigger carries — not from a resolution context that no longer exists. The same
 * carried number then re-checks the target on resolution (CR 608.2b).
 */
class ReflexiveTargetStoredNumberCapTest : FunSpec({

    /** "Store 2. When you do, destroy target creature an opponent controls with mana value 2 or less." */
    val CapSpell = card("Stored Cap Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature an opponent controls with mana value N or less."
        spell {
            effect = Effects.Pipeline {
                val n = runStoringNumber { Effects.StoreNumber(it, DynamicAmounts.fixed(2)) }
                run(Effects.ReflexiveTrigger(action = Effects.Nothing, optional = false) {
                    val creature = target(
                        TargetFilter(GameObjectFilter.Creature.opponentControls().manaValueAtMostDynamic(n.amount))
                    )
                    effect = Effects.Destroy(creature)
                })
            }
        }
    }

    fun creature(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Creature — Bear"
        power = 1
        toughness = 1
    }

    val OneDrop = creature("Stored Cap One-Drop", "{1}")
    val TwoDrop = creature("Stored Cap Two-Drop", "{2}")
    val ThreeDrop = creature("Stored Cap Three-Drop", "{3}")

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CapSpell, OneDrop, TwoDrop, ThreeDrop))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    test("targets are capped by the stored number when the reflexive trigger goes on the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val one = d.putCreatureOnBattlefield(opp, "Stored Cap One-Drop")
        val two = d.putCreatureOnBattlefield(opp, "Stored Cap Two-Drop")
        val three = d.putCreatureOnBattlefield(opp, "Stored Cap Three-Drop")
        d.putCreatureOnBattlefield(you, "Stored Cap One-Drop")

        d.castSpell(you, d.putCardInHand(you, "Stored Cap Test Spell")).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets.getValue(0) shouldContainExactlyInAnyOrder listOf(one, two)

        d.submitTargetSelection(you, listOf(two)).error shouldBe null
        d.bothPass()
        d.findPermanent(opp, "Stored Cap Two-Drop") shouldBe null
        d.findPermanent(opp, "Stored Cap One-Drop") shouldBe one
        d.findPermanent(opp, "Stored Cap Three-Drop") shouldBe three
    }

    test("with nothing under the cap the reflexive trigger has no legal target and never goes on the stack") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val three = d.putCreatureOnBattlefield(opp, "Stored Cap Three-Drop")

        d.castSpell(you, d.putCardInHand(you, "Stored Cap Test Spell")).error shouldBe null
        d.bothPass()

        d.pendingDecision shouldBe null
        d.state.stack shouldBe emptyList()
        d.findPermanent(opp, "Stored Cap Three-Drop") shouldBe three
    }
})
