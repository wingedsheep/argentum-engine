package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Devoid (CR 702.114a) at the model layer, where the SDK puts it: a characteristic-defining
 * ability that empties [CardDefinition.colors] rather than a continuous effect.
 *
 * These sit next to the `colors` getter for the same reason [ColorIdentityTest] sits next to
 * `colorIdentity` — the rule *is* the getter, and every zone-agnostic reader in the engine, the
 * server and the client inherits whatever this file pins down. The engine's own side of it (all
 * five zones, layer 5, evasion, copies) is `DevoidKeywordScenarioTest` in `rules-engine`.
 */
class DevoidColorsTest : DescribeSpec({

    /** Ulamog's Nullifier: {2}{U}{U}, devoid — the shape the whole BFZ/OGW cycle takes. */
    fun devoidCreature(
        keywords: Set<Keyword> = setOf(Keyword.DEVOID),
        keywordAbilities: List<KeywordAbility> = emptyList(),
    ) = CardDefinition.creature(
        name = "Devoid Eldrazi",
        manaCost = ManaCost.parse("{2}{U}{U}"),
        subtypes = setOf(Subtype("Eldrazi")),
        power = 2,
        toughness = 3,
        oracleText = "Devoid (This card has no color.)",
        keywords = keywords,
    ).copy(keywordAbilities = keywordAbilities)

    describe("colors") {
        it("a devoid card is colorless despite its coloured pips (CR 702.114a)") {
            devoidCreature().colors shouldBe emptySet()
        }

        it("the same card without devoid is its mana cost's colours") {
            devoidCreature(keywords = emptySet()).colors shouldBe setOf(Color.BLUE)
        }

        it("reads devoid in either SDK spelling — the keyword set or a keyword ability") {
            devoidCreature(
                keywords = emptySet(),
                keywordAbilities = listOf(KeywordAbility.of(Keyword.DEVOID)),
            ).colors shouldBe emptySet()
        }

        it("overrides an explicit colour indicator too — the CDA says colorless, full stop") {
            val indicated = devoidCreature().copy(colorIndicator = setOf(Color.BLACK))
            indicated.colors shouldBe emptySet()
            // …and the indicator is still honoured on a card that isn't devoid.
            indicated.copy(keywords = emptySet()).colors shouldBe setOf(Color.BLUE, Color.BLACK)
        }

        it("leaves an already-colorless card alone") {
            val eldrazi = CardDefinition.creature(
                name = "Colorless Eldrazi",
                manaCost = ManaCost.parse("{4}"),
                subtypes = setOf(Subtype("Eldrazi")),
                power = 4,
                toughness = 4,
                keywords = setOf(Keyword.DEVOID),
            )
            eldrazi.colors shouldBe emptySet()
        }
    }

    describe("color identity (CR 903.4)") {
        it("is untouched by devoid — a colorless card can still be blue-identity") {
            devoidCreature().colorIdentity shouldBe setOf(Color.BLUE)
        }

        it("still picks up colours from the rules text of a devoid card") {
            val card = CardDefinition.creature(
                name = "Devoid Activator",
                manaCost = ManaCost.parse("{2}"),
                subtypes = setOf(Subtype("Eldrazi")),
                power = 1,
                toughness = 1,
                oracleText = "Devoid (This card has no color.)\n{B}: This creature gets +1/+0.",
                keywords = setOf(Keyword.DEVOID),
            )
            card.colors shouldBe emptySet()
            card.colorIdentity shouldBe setOf(Color.BLACK)
        }
    }
})
