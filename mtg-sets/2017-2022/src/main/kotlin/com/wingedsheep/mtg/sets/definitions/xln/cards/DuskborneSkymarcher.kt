package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Duskborne Skymarcher
 * {W}
 * Creature — Vampire Cleric
 * 1/1
 * Flying
 * {W}, {T}: Target attacking Vampire gets +1/+1 until end of turn.
 */
val DuskborneSkymarcher = card("Duskborne Skymarcher") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Cleric"
    power = 1
    toughness = 1
    oracleText = "Flying\n{W}, {T}: Target attacking Vampire gets +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val vampire = target(TargetFilter(GameObjectFilter.Creature.attacking().withSubtype("Vampire")))
        effect = Effects.ModifyStats(1, 1, vampire)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Seb McKinnon"
        flavorText = "\"The hour of Dusk is come.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/a/baa524bb-0ce7-4c30-93c3-ae15c3b05e92.jpg?1783935803"
    }
}
