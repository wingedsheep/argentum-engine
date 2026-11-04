package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dire Fleet Poisoner
 * {1}{B}
 * Creature — Human Pirate
 * 2/2
 * Flash
 * Deathtouch
 * When this creature enters, target attacking Pirate you control gets +1/+1 and gains deathtouch
 * until end of turn.
 *
 * Only creatures can attack, so the "attacking Pirate" is a Pirate creature.
 */
val DireFleetPoisoner = card("Dire Fleet Poisoner") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    oracleText = "Flash\nDeathtouch\n" +
        "When this creature enters, target attacking Pirate you control gets +1/+1 and gains " +
        "deathtouch until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLASH, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val pirate = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.PIRATE).attacking().youControl())
        )
        effect = Effects.ModifyStats(1, 1, pirate) then Effects.GrantKeyword(Keyword.DEATHTOUCH, pirate)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Ben Maier"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88273b1e-b33b-44d3-8777-4e78a04eed65.jpg?1783935313"
    }
}
