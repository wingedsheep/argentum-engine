package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scavenging Harpy
 * {2}{B}
 * Creature — Harpy
 * 2/1
 * Flying
 * When this creature enters, exile target card from an opponent's graveyard.
 *
 * Leonin of the Lost Pride's graveyard-hate target on an enters trigger instead of a dies trigger.
 */
val ScavengingHarpy = card("Scavenging Harpy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Harpy"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, exile target card from an opponent's graveyard."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(card)
        description = "When this creature enters, exile target card from an opponent's graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Daniel Ljunggren"
        flavorText = "Desecrating the dead is a convenient way for harpies to expand their foul collections."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0e237c5-45b4-49df-adb9-62b9f3b62986.jpg?1783931561"
    }
}
