package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * `GameObjectFilter.description` renders a controller predicate as a **prefix** ("you control
 * land"), which is fine for a debug string and wrong for player-facing text. The mana statics below
 * interpolate a filter straight into a sentence the client shows on a granted ability, so Deep Water
 * read "If a you control land is tapped for mana…" in play.
 *
 * They render through [describeObjectForEvent] instead, which supplies the article and keeps the
 * controller clause a suffix — the same describer the event wordings use. These cases pin both the
 * word order and the article, so switching either back fails here rather than in a screenshot.
 */
class ManaStaticDescriptionTest : DescribeSpec({

    describe("ReplaceLandManaColor.description") {

        it("keeps the controller clause after the noun (Deep Water)") {
            val description = ReplaceLandManaColor(
                filter = GameObjectFilter.Land.youControl(),
                color = Color.BLUE,
            ).description

            description shouldBe
                "If a land you control is tapped for mana, it produces {U} instead of any other type"
            description shouldNotContain "you control land"
        }

        it("renders the free-choice form the same way (Pulse of Llanowar)") {
            ReplaceLandManaColor(filter = GameObjectFilter.Land.youControl()).description shouldBe
                "If a land you control is tapped for mana, it produces mana of a color of its " +
                "controller's choice instead of any other type"
        }

        it("still reads correctly with no controller clause at all") {
            ReplaceLandManaColor(filter = GameObjectFilter.Land, color = Color.BLUE).description shouldBe
                "If a land is tapped for mana, it produces {U} instead of any other type"
        }
    }

    describe("MultiplyManaOnSourceTap.description") {

        it("keeps the controller clause after the noun") {
            val description = MultiplyManaOnSourceTap(
                sourceFilter = GameObjectFilter.Land.youControl(),
                multiplier = 3,
            ).description

            description shouldBe
                "If you tap a land you control for mana, it produces 3 times as much of that mana instead"
            description shouldNotContain "you control land"
        }

        it("picks the article from the noun, not from the controller clause") {
            // "an artifact you control", never "a artifact" — the article is derived from the type
            // word the describer renders first, and the controller clause is not that word.
            MultiplyManaOnSourceTap(
                sourceFilter = GameObjectFilter.Artifact.youControl(),
                multiplier = 2,
            ).description shouldBe
                "If you tap an artifact you control for mana, it produces 2 times as much of that mana instead"
        }
    }
})
