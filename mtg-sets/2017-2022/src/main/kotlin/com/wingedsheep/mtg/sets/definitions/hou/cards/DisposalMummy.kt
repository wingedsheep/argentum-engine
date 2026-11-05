package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Disposal Mummy
 * {2}{W}
 * Creature — Zombie Jackal
 * 2/3
 * When this creature enters, exile target card from an opponent's graveyard.
 */
val DisposalMummy = card("Disposal Mummy") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Zombie Jackal"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, exile target card from an opponent's graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(exiled)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Gabor Szikszai"
        flavorText = "Without the viziers to oversee their actions, some anointed went to extreme lengths to fulfill their directives."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad806d67-4c98-438b-859f-b1358281e09d.jpg?1783936067"
    }
}
