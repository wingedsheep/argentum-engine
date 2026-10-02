package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kudo, King Among Bears
 * {G}{W}
 * Legendary Creature — Bear
 * 2/2
 *
 * Other creatures have base power and toughness 2/2 and are Bears in addition to their other types.
 *
 * One printed static ability spanning Layer 4 (Bear subtype) and Layer 7b (base 2/2), bundled into a
 * [CompositeStaticAbility] so both parts share one affected set (CR 613.6). Counters and +N/+N
 * modifiers (Layer 7c) still apply on top; later-timestamped P/T-setting effects override it.
 */
val KudoKingAmongBears = card("Kudo, King Among Bears") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Bear"
    power = 2
    toughness = 2
    oracleText = "Other creatures have base power and toughness 2/2 and are Bears in addition to their other types."

    staticAbility {
        ability = CompositeStaticAbility(
            listOf(
                SetBasePowerToughnessStatic(2, 2, GroupFilter.AllOtherCreatures),
                GrantSubtype("Bear", GroupFilter.AllOtherCreatures),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Ekaterina Burmak"
        flavorText = "Born from the greatest granite boulder, nursed on the sweetest honey, crowned amid a raging blizzard."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/585e4e02-4873-4a65-a9ad-30cf0d5b6f79.jpg?1783911248"

        ruling(
            "2024-06-07",
            "Kudo, King Among Bears's ability overwrites any effects that set a creature's power and toughness. " +
                "Any existing effects or counters that raise, lower, or switch a creature's power and/or toughness " +
                "continue to apply to the creature's newly set power and toughness. Any power- or toughness-setting " +
                "effects that start to apply after Kudo enters the battlefield will overwrite this effect."
        )
        ruling(
            "2024-06-07",
            "If an effect causes a noncreature permanent to become a creature and sets its power and toughness as " +
                "it does so, that creature will have that power and toughness; it won't be 2/2. Notably, crewing a " +
                "Vehicle does not set its power and toughness, so a Vehicle will be a 2/2 creature once crewed."
        )
        ruling(
            "2024-06-07",
            "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal " +
                "damage dealt to creatures may become lethal if Kudo enters or leaves the battlefield during that turn."
        )
    }
}
