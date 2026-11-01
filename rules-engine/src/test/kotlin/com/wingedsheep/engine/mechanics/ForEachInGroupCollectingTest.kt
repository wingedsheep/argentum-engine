package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `ForEachInGroupCollectingEffect` (the `forEachInGroupCollecting { }` pipeline verb) with
 * `storeEntity`: one body per battlefield object of the group, each recording the visited object
 * into a body-local collection, and the per-member collections unioned and published after the loop
 * so a single follow-up acts on them all.
 *
 * Covers both seams the aggregate travels through — a synchronous body (the result's
 * `updatedCollections`) and a body that pauses for a card choice (the continuation resumer).
 */
class ForEachInGroupCollectingTest : FunSpec({

    // Synchronous body: record each creature with power 2 or less, then destroy them all.
    val cullTheSmall = card("Cull the Small") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "For each creature, note it if its power is 2 or less. Destroy each noted creature."
        spell {
            effect = Effects.Pipeline {
                val (small) = forEachInGroupCollecting(GroupFilter(GameObjectFilter.Creature)) {
                    listOf(
                        storeEntity(
                            onlyIf = Conditions.CompareAmounts(
                                DynamicAmounts.powerOf(EffectTarget.IterationEntity), ComparisonOperator.LTE, 2
                            )
                        )
                    )
                }
                destroy(small)
            }
        }
    }

    // Pausing body: for each creature, you may choose it; destroy every chosen creature.
    val pickAndPurge = card("Pick and Purge") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "For each creature, you may choose it. Destroy each chosen creature."
        spell {
            effect = Effects.Pipeline {
                val (chosen) = forEachInGroupCollecting(GroupFilter(GameObjectFilter.Creature)) {
                    listOf(chooseUpTo(1, from = storeEntity(), alwaysPrompt = true))
                }
                destroy(chosen)
            }
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(cullTheSmall, pickAndPurge))
        initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a synchronous body publishes every member it recorded, and only those") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Savannah Lions")
        val courser = d.putCreatureOnBattlefield(me, "Centaur Courser")
        d.putCreatureOnBattlefield(them, "Savannah Lions")
        val spell = d.putCardInHand(me, "Cull the Small")
        d.giveMana(me, Color.BLACK, 1)

        d.castSpell(me, spell)
        d.bothPass()

        withClue("both 2/1s across both sides were destroyed; the 3/3 was never recorded") {
            d.getGraveyardCardNames(me) shouldContainExactlyInAnyOrder listOf("Cull the Small", "Savannah Lions")
            d.getGraveyardCardNames(them) shouldBe listOf("Savannah Lions")
            d.getPermanents(me).contains(courser) shouldBe true
        }
    }

    test("a pausing body carries each iteration's pick to the follow-up") {
        val d = driver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val lions = d.putCreatureOnBattlefield(me, "Savannah Lions")
        val courser = d.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirLions = d.putCreatureOnBattlefield(them, "Savannah Lions")
        val spell = d.putCardInHand(me, "Pick and Purge")
        d.giveMana(me, Color.BLACK, 1)

        d.castSpell(me, spell)
        d.bothPass()

        // One prompt per creature, each offering only the creature being visited.
        val keep = setOf(courser)
        repeat(3) {
            val decision = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            val visited = decision.options.single()
            d.submitCardSelection(me, if (visited in keep) emptyList() else listOf(visited))
        }

        withClue("the two chosen creatures were destroyed together; the declined one survived") {
            d.state.pendingDecision shouldBe null
            d.getGraveyardCardNames(me) shouldContainExactlyInAnyOrder listOf("Pick and Purge", "Savannah Lions")
            d.getGraveyardCardNames(them) shouldBe listOf("Savannah Lions")
            d.getPermanents(me).filter { it == lions || it == courser } shouldBe listOf(courser)
            d.getPermanents(them).contains(theirLions) shouldBe false
        }
    }
})
