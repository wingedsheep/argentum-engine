package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Will of the Temur — Tarkir: Dragonstorm Commander #24 (canonical printing)
 * {5}{U} · Sorcery
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Create a token that's a copy of target permanent, except it's a 4/4 Dragon creature with
 *   flying in addition to its other types.
 * • Target player draws cards equal to the greatest mana value among permanents you control.
 *
 * The conditional mode count is the Will cycle's shape (Will of the Jeskai / Sultai):
 * `minChooseCount = 1`, capped at 2 when [Conditions.YouControlACommander] holds as it is cast.
 *
 * Mode one is Applied Geometry's "copy, except it's a P/T <subtype> creature in addition to its
 * other types": [Effects.CreateTokenCopyOfTarget] with a 4/4 override, CREATURE unioned onto the
 * copied card types, Dragon added to the copied subtypes and flying granted as part of the copy.
 * Mode two is Rush of Knowledge's greatest-mana-value count aimed at a target player, read as the
 * mode resolves — after mode one's token exists when both are chosen (printed order).
 */
val WillOfTheTemur = card("Will of the Temur") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Create a token that's a copy of target permanent, except it's a 4/4 Dragon creature with " +
        "flying in addition to its other types.\n" +
        "• Target player draws cards equal to the greatest mana value among permanents you control."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Create a token that's a copy of target permanent, except it's a 4/4 Dragon creature with flying in addition to its other types") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.CreateTokenCopyOfTarget(
                    target = permanent,
                    overridePower = 4,
                    overrideToughness = 4,
                    addCardTypes = setOf("CREATURE"),
                    addedSubtypes = setOf(Subtype.DRAGON),
                    addedKeywords = setOf(Keyword.FLYING),
                )
            }
            mode("Target player draws cards equal to the greatest mana value among permanents you control") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(DynamicAmounts.battlefield(Player.You).maxManaValue(), player)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d236aa15-f60c-4368-b796-4d41e0bc3358.jpg?1783907179"
        ruling("2025-04-04", "The token created by Will of the Temur's first mode copies exactly what was printed on the original permanent and nothing else, with the listed exceptions (unless that permanent is copying something else or is a token). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on.")
        ruling("2025-04-04", "If a permanent has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-04-04", "The commander you control doesn't have to be your commander.")
        ruling(
            "2025-04-04",
            "Whether or not you control a commander is determined only once, as you choose the modes for this " +
                "spell. If you somehow lose control of that commander before you finish casting the spell " +
                "(perhaps because you sacrifice it to activate a mana ability), it won't change the number of " +
                "modes chosen."
        )
    }
}
