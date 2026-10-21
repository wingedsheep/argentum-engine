package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Coppercoat Vanguard — March of the Machine: The Aftermath #1
 * {1}{W} · Creature — Human Soldier · 2/2
 *
 * Each other Human you control gets +1/+0 and has ward {1}.
 *
 * A two-part Human lord over one "other" group: a flat [ModifyStats] anthem plus a [GrantWard]
 * with a mana ward cost. `excludeSelf` keeps the Vanguard itself out of both grants.
 */
private val OtherHumansYouControl =
    GroupFilter(GameObjectFilter.Permanent.withSubtype("Human").youControl(), excludeSelf = true)

val CoppercoatVanguard = card("Coppercoat Vanguard") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Each other Human you control gets +1/+0 and has ward {1}. (Whenever it becomes " +
        "the target of a spell or ability an opponent controls, counter it unless that player pays {1}.)"

    staticAbility {
        ability = ModifyStats(1, 0, OtherHumansYouControl)
    }
    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"), OtherHumansYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Bruno Biazotto"
        flavorText = "It was a time for rebuilding, but first Ikoria's wrecked cities had to be cleared of monsters."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d786f05d-78a2-41b6-a185-111e8c1b216b.jpg?1783916531"
    }
}
