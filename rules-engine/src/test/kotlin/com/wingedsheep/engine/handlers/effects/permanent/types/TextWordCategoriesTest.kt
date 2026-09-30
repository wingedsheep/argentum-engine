package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ChangeWordInTextEffect
import com.wingedsheep.sdk.scripting.effects.TextWordCategory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class TextWordCategoriesTest : FunSpec({
    val player = EntityId("player")
    val target = EntityId("target")
    val definition = card("Word Witness") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        oracleText = "Forestwalk, islandwalk"
        power = 2
        toughness = 2
        keywords(Keyword.FORESTWALK, Keyword.ISLANDWALK)
    }
    val state = GameState(
        entities = mapOf(target to CardEntityFactory.create(definition, player)),
        zones = mapOf(ZoneKey(player, Zone.BATTLEFIELD) to listOf(target))
    )
    val context = EffectContext(sourceId = null, controllerId = player, targets = listOf(ChosenTarget.Permanent(target)))
    val executor = ChangeWordInTextExecutor()

    for (categories in listOf(setOf(TextWordCategory.COLOR_WORD), setOf(TextWordCategory.BASIC_LAND_TYPE), TextWordCategory.entries.toSet())) {
        test("$categories restricts both word lists and every legal replacement") {
            val result = executor.execute(state, ChangeWordInTextEffect(categories, duration = Duration.Permanent), context)
            val decision = result.pendingDecision as ChooseReplacementDecision
            val expected = ChangeWordInTextExecutor.wordsFor(categories)
            decision.fromOptions.shouldContainExactlyInAnyOrder(expected)
            decision.toOptions shouldBe expected
            decision.allowedToByFrom.forEachIndexed { index, allowed ->
                allowed.size shouldBe 4
                val from = decision.fromOptions[index]
                val colors = listOf("White", "Blue", "Black", "Red", "Green")
                allowed.map { decision.toOptions[it] }.forEach { to ->
                    (to in colors) shouldBe (from in colors)
                    (to == from) shouldBe false
                }
            }
            state.pendingDecision shouldBe null
        }
    }
    test("target outside battlefield and stack does not ask for a choice") {
        val outside = state.copy(zones = mapOf(ZoneKey(player, Zone.HAND) to listOf(target)))
        executor.execute(outside, ChangeWordInTextEffect(setOf(TextWordCategory.COLOR_WORD)), context).pendingDecision shouldBe null
    }
    test("landwalk replacements preserve multiple original abilities and chain in effect order") {
        val changes = TextReplacementComponent(listOf(
            TextReplacement("Forest", "Island", TextReplacementCategory.BASIC_LAND_TYPE),
            TextReplacement("Island", "Swamp", TextReplacementCategory.BASIC_LAND_TYPE)
        ))
        val changed = state.updateEntity(target) { it.with(changes) }
        changed.projectedState.getKeywords(target) shouldBe setOf("SWAMPWALK")
        state.projectedState.getKeywords(target) shouldBe setOf("FORESTWALK", "ISLANDWALK")
    }
})
