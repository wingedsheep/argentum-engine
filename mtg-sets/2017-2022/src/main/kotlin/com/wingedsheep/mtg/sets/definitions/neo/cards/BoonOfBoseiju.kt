package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Boon of Boseiju — Kamigawa: Neon Dynasty #176 (canonical printing)
 * {1}{G} · Instant
 *
 * Target creature gets +X/+X until end of turn, where X is the greatest mana value among
 * permanents you control. Untap it.
 *
 * X is read once, as the spell resolves (per the ruling), by the battlefield MAX aggregate.
 */
val BoonOfBoseiju = card("Boon of Boseiju") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +X/+X until end of turn, where X is the greatest mana value among permanents you control. Untap it."

    spell {
        val t = target(TargetFilter.Creature)
        val x = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent).maxManaValue()
        effect = Effects.ModifyStats(x, x, t) then Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Anastasia Ovchinnikova"
        flavorText = "She closed her eyes and remembered the smell of the leaves of Jukai Forest. When she opened them, the buildings of Towashi had grown small."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06771591-0480-4232-bc67-216b2e0fe738.jpg?1783923854"
        ruling("2022-02-18", "The greatest mana value among permanents you control is determined as Boon of Boseiju resolves.")
        ruling("2022-02-18", "If a permanent on the battlefield has {X} in its mana cost, X is 0 when determining that permanent's mana value.")
        ruling("2022-02-18", "Transforming double-faced permanents, such as those found in this set, have the mana value of their front face at all times, even while back-face-up.")
    }
}
