package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Argivian Archaeologist
 * {1}{W}{W}
 * Creature — Human Artificer
 * 1/1
 * {W}{W}, {T}: Return target artifact card from your graveyard to your hand.
 */
val ArgivianArchaeologist = card("Argivian Archaeologist") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 1
    oracleText = "{W}{W}, {T}: Return target artifact card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{W}"), Costs.Tap)
        val artifact = target(
            "target artifact card from your graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD))
        )
        effect = Effects.ReturnToHand(artifact)
        description = "{W}{W}, {T}: Return target artifact card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Amy Weber"
        flavorText = "Fascinated by the lore of ancient struggles, the Archaeologist searches incessantly for remnants of an earlier, more powerful era."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce83a3cb-467d-44f6-a051-4855c8cf52a6.jpg?1562938661"
    }
}
