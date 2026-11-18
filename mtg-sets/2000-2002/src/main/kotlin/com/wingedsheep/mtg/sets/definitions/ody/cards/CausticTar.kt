package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Caustic Tar
 * {4}{B}{B}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Target player loses 3 life."
 */
val CausticTar = card("Caustic Tar") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Target player loses 3 life.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val p = target(Targets.Player)
                effect = Effects.LoseLife(3, p)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "John Avon"
        flavorText = "\"Twisted magic, twisted fate, Force the land to ooze with hate.\"\n—Cabal chant"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3700a61f-76fe-42e6-be93-0ba319b7b543.jpg?1783945250"
    }
}
