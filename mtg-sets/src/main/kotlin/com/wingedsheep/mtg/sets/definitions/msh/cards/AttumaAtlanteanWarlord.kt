package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Attuma, Atlantean Warlord
 * {2}{U}{U}
 * Legendary Creature — Merfolk Warrior Villain
 * 3/4
 *
 * Other Merfolk you control get +1/+1.
 * Whenever one or more Merfolk you control attack a player, draw a card.
 *
 * The lord is a Layer 7c [ModifyStats] over a `excludeSelf` [GroupFilter] of Merfolk you control
 * (Deepchannel Duelist idiom). The attack trigger is [Triggers.YouAttackWithFilter], which fires
 * once per declare-attackers regardless of how many Merfolk attacked (Meriadoc Brandybuck idiom).
 */
val AttumaAtlanteanWarlord = card("Attuma, Atlantean Warlord") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk Warrior Villain"
    power = 3
    toughness = 4
    oracleText = "Other Merfolk you control get +1/+1.\n" +
        "Whenever one or more Merfolk you control attack a player, draw a card."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Merfolk").youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(
            GameObjectFilter.Creature.youControl().withSubtype("Merfolk")
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Nanna Marie Steffensen"
        flavorText = "\"All who swim the tides shall bow to my command!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d5add1-0d0e-414b-a964-8da326472d35.jpg?1783902962"
    }
}
