package com.wingedsheep.tooling.coverage

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonElement

/**
 * Locks the typed IR matcher ([IrQuery]) — the scope-correct accessors the emitter's filter / trigger /
 * amount handlers read the mtgish IR through, replacing the `compact()`+regex/substring idiom. Each spec
 * pins both the value the accessor recovers AND the scope discipline that a flattened-blob match lacked
 * (a tag is matched as a discriminator value, not as an arbitrary substring of the serialised subtree).
 */
class IrQueryTest : StringSpec({

    fun node(json: String): JsonElement = J.parseToJsonElement(json)

    "argWordsTagged collects each tagged node's word arg in document order" {
        // The `Or[Goblin, Soldier]` creature-subtype shape: two IsCreatureType nodes.
        val filter = node(
            """{"_Filter":"Or","args":[""" +
                """{"_Permanents":"IsCreatureType","args":"Goblin"},""" +
                """{"_Permanents":"IsCreatureType","args":"Soldier"}]}""",
        )
        filter.argWordsTagged("IsCreatureType") shouldBe listOf("Goblin", "Soldier")
        filter.firstArgWordTagged("IsCreatureType") shouldBe "Goblin"
        filter.argWordsTagged("IsCardtype") shouldBe emptyList()
    }

    "argWordsTagged skips non-word args, mirroring the (\\w+) capture" {
        // An args that is a nested object (not a string word) does not count.
        val n = node("""{"_Permanents":"IsCreatureType","args":{"_Ref":"ThatCreature"}}""")
        n.argWordsTagged("IsCreatureType") shouldBe emptyList()
    }

    "hasTag matches a discriminator value, not a substring of a longer tag" {
        node("""{"_Permanents":"IsTapped"}""").hasTag("IsTapped").shouldBeTrue()
        // "IsUntapped" must NOT satisfy hasTag("IsTapped") — substring `"IsTapped" in blob` likewise
        // wouldn't, but a careless `Tapped` check would; the typed form is exact by construction.
        node("""{"_Permanents":"IsUntapped"}""").hasTag("IsTapped").shouldBeFalse()
        node("""{"_Permanents":"IsUntapped"}""").hasTag("IsUntapped").shouldBeTrue()
    }

    "wordsAtKey / firstColorOf read values at a key, scoping the colour to its clause" {
        val anyColor = node(
            """{"_Filter":"Or","args":[""" +
                """{"_Permanents":"IsColor","args":{"_Color":"White"}},""" +
                """{"_Permanents":"IsColor","args":{"_Color":"Blue"}}]}""",
        )
        anyColor.wordsAtKey("_Color") shouldBe listOf("White", "Blue")
        anyColor.firstColorOf("IsColor") shouldBe "White"

        val nonColor = node("""{"_Permanents":"IsNonColor","args":{"_Color":"Red"}}""")
        nonColor.firstColorOf("IsNonColor") shouldBe "Red"
        nonColor.firstColorOf("IsColor").shouldBeNull()  // IsColor must not match IsNonColor
    }

    "hasStringValue matches a string VALUE anywhere, the typed form of a quoted-token blob check" {
        val flying = node("""{"_Permanents":"DoesntHaveAbility","args":[{"_Keyword":"Flying"}]}""")
        flying.hasStringValue("Flying").shouldBeTrue()
        flying.hasStringValue("Trample").shouldBeFalse()
    }

    "firstArgStringTagged returns the full arg string (a [^\"]+ capture, not just a word)" {
        val n = node("""{"_CardsInLibrary":"IsLandType","args":"Island"}""")
        n.firstArgStringTagged("IsLandType") shouldBe "Island"
        n.firstArgStringTagged("IsCreatureType").shouldBeNull()
    }
})
