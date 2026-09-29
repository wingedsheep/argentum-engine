package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Marshal of Zhalfir (March of the Machine #246)
 * {W}{U} Creature — Human Knight 2/2
 * Other Knights you control get +1/+1.
 * {W}{U}, {T}: Tap another target creature.
 *
 * "Knights" is a bare tribal noun, so the lord reads Knight *permanents* (as Valiant Knight does).
 */
val MarshalOfZhalfir = card("Marshal of Zhalfir") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Other Knights you control get +1/+1.\n{W}{U}, {T}: Tap another target creature."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{U}"), Costs.Tap)
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.Tap(creature)
        description = "Tap another target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Darren Tan"
        flavorText = "\"How kind of our foes to move in such perfect, predictable unison. Split into squadrons and execute the Lion's Claw!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2418f5c-a85f-4249-8a53-c94b011b9714.jpg?1783916943"
    }
}
