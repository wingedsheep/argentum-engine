package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spell Rupture
 * {1}{U}
 * Instant
 * Counter target spell unless its controller pays {X}, where X is the greatest power among
 * creatures you control.
 *
 * [Effects.CounterUnlessDynamicPays] over the MAX-power aggregate of your creatures — the amount is
 * read on resolution, so with no creatures X is 0 and the tax is {0}.
 */
val SpellRupture = card("Spell Rupture") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}, where X is the greatest power among creatures you control."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Kev Walker"
        flavorText = "\"Like water, mana will drain from an inferior vessel.\"\n—Jalan Tosk, Simic forcemage"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7267fcec-0879-4743-a45f-35057ccb2596.jpg?1783940134"

        ruling(
            "2013-01-24",
            "The value of X is the greatest power among creatures you control when Spell Rupture " +
                "resolves. If you control no creatures at that time, X will be 0. The target spell's " +
                "controller can choose to pay {0} (simply by indicating they wish to do so). That " +
                "player can also choose to not pay {0} and the spell will be countered."
        )
    }
}
