package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Leonin Lightbringer — Phyrexia: All Will Be One #20
 * {2}{W} · Creature — Cat Rebel · 3/2
 *
 * Ward {2}
 * As long as this creature is equipped, it gets +1/+1.
 *
 * The equipped gate is [Conditions.SourceMatches] over `equipped()` (Leonin Den-Guard's shape), so
 * attaching or removing Equipment flips the bonus during projection with no trigger.
 */
val LeoninLightbringer = card("Leonin Lightbringer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Rebel"
    power = 3
    toughness = 2
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\n" +
        "As long as this creature is equipped, it gets +1/+1."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.equipped())
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Ernanda Souza"
        flavorText = "\"It's hard to fight for the future if you can't see the way forward.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96ae13df-670e-49a0-99b0-baf908ac9eb4.jpg?1783918079"
    }
}
