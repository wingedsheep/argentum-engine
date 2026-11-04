package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

// Oracle errata allows the first target to be a planeswalker directly.
val BurningSunsAvatar = card("Burning Sun's Avatar") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur Avatar"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, it deals 3 damage to target opponent or planeswalker and 3 damage to up to one target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponentOrPlaneswalker = target(Targets.OpponentOrPlaneswalker)
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.DealDamage(3, opponentOrPlaneswalker) then Effects.DealDamage(3, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Randy Vargas"
        flavorText = "\"Tilonalli's searing rays carry the spark of life's eventual destruction as all light fades.\"\n—Chitlati, Lightning Rider"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/146c0cab-5f6b-425d-9f50-33d8da266235.jpg?1783935749"
        ruling(
            "2017-09-29",
            "The ability of Burning Sun’s Avatar can target any creature, not just one controlled by the target opponent."
        )
        ruling(
            "2017-09-29",
            "If the ability of Burning Sun’s Avatar has two targets and one becomes illegal, the other will still be dealt damage."
        )
    }
}
