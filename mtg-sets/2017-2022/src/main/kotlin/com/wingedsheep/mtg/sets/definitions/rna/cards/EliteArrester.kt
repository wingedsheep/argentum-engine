package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elite Arrester — Ravnica Allegiance #266
 * {W} · Creature — Human Soldier · 0 / 3
 *
 * A one-drop tapper with an off-colour Azorius activation.
 */
val EliteArrester = card("Elite Arrester") {
    manaCost = "{W}"
    colorIdentity = "UW"
    typeLine = "Creature — Human Soldier"
    power = 0
    toughness = 3
    oracleText = "{1}{U}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "266"
        inBooster = false
        artist = "Randy Vargas"
        flavorText = "\"Hold it! I need to see your papers.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/070f0a21-8e06-46ec-9d84-c65067b23893.jpg"
    }
}
