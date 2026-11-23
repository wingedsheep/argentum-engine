package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Skygames
 * {1}{U}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Target creature gains flying until end of turn. Activate only as a sorcery."
 */
val Skygames = card("Skygames") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Target creature gains flying until end of turn. " +
        "Activate only as a sorcery.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter.Creature)
                effect = Effects.GrantKeyword(Keyword.FLYING, creature)
                timing = TimingRule.SorcerySpeed
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ab5bf75-762f-46ef-8304-aacdb248bc5b.jpg?1783940135"
    }
}
