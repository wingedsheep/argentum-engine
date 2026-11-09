package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crossroads Consecrator — Eldritch Moon #154
 * {G} · Creature — Human Cleric · 1/2
 *
 * {G}, {T}: Target attacking Human gets +1/+1 until end of turn.
 */
val CrossroadsConsecrator = card("Crossroads Consecrator") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "{G}, {T}: Target attacking Human gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val human = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Human").attacking()))
        effect = Effects.ModifyStats(1, 1, human)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "David Gaillet"
        flavorText = "\"May the steel of your blade match the steel in your soul.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1a83854-dc45-4c26-aca0-67f372236383.jpg?1783937453"
    }
}
