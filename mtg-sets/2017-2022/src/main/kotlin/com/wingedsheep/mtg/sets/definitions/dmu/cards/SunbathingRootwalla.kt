package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sunbathing Rootwalla
 * {1}{G}
 * Creature — Lizard
 * 2/2
 * Domain — {3}{G}: Until end of turn, this creature gets +1/+1 for each basic land type among
 * lands you control. Activate only once each turn.
 */
val SunbathingRootwalla = card("Sunbathing Rootwalla") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    power = 2
    toughness = 2
    oracleText = "Domain — {3}{G}: Until end of turn, this creature gets +1/+1 for each basic land type among lands you control. Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.ModifyStats(DynamicAmounts.domain(), DynamicAmounts.domain(), EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Campbell White"
        flavorText = "A rootwalla's prey includes anything or anyone that wanders into view."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ae6f015-2ad6-4b8e-8590-c2c2d01b6d98.jpg?1783921295"
    }
}
