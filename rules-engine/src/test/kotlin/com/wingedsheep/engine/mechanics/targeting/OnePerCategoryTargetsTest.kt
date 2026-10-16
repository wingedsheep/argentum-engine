package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.constrainedTargetSelection
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * "Up to one target … of each color / card type" from the chooser's side: the category table the
 * engine hands to choosers, and the shared [constrainedTargetSelection] the AI and the minimal
 * selection use, which must only ever build a selection whose picks each claim a different category.
 */
class OnePerCategoryTargetsTest : FunSpec({

    val whiteBlue = EntityId.of("white-blue")
    val white = EntityId.of("white")
    val blue = EntityId.of("blue")
    val red = EntityId.of("red")
    val red2 = EntityId.of("red-2")
    val colorless = EntityId.of("colorless")
    val table = mapOf(
        whiteBlue to listOf("BLUE", "WHITE"),
        white to listOf("WHITE"),
        blue to listOf("BLUE"),
        red to listOf("RED"),
        red2 to listOf("RED"),
    )
    val pool = listOf(whiteBlue, white, blue, red, red2, colorless)

    test("the cap is the largest matching over the candidates") {
        OnePerCategoryTargets.maxTargets(table) shouldBe 3
        OnePerCategoryTargets.maxTargets(mapOf(red to listOf("RED"), red2 to listOf("RED"))) shouldBe 1
    }

    test("the selection takes the best-ranked picks that still claim distinct categories") {
        val rank = mapOf(red to 5.0, red2 to 4.0, whiteBlue to 3.0, white to 2.0, blue to 1.0, colorless to 9.0)
        val picks = constrainedTargetSelection(pool, 0, 5, null, null, { rank.getValue(it) }, table)
        // The colourless card ranks highest but claims nothing; the second red is shut out by the
        // first; the white-blue card takes blue so white still fits; plain blue no longer does.
        picks shouldContainExactlyInAnyOrder listOf(red, whiteBlue, white)
    }

    test("without categories the helper keeps its owner-group behaviour") {
        val picks = constrainedTargetSelection(listOf(white, blue, red), 2, 2, null, listOf(listOf(white, blue), listOf(red)))
        picks.size shouldBe 2
        (red in picks) shouldBe true
    }
})
