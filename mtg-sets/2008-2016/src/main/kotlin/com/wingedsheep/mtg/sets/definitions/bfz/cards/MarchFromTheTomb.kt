package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * March from the Tomb {3}{W}{B}
 * Sorcery
 *
 * Return any number of target Ally creature cards with total mana value 8 or less from your
 * graveyard to the battlefield.
 *
 * The Ally creature cards are real targets chosen as the spell is cast (CR 601.2c), any number of
 * them (zero included), with their summed mana value capped by `totalAtMost`. Any target that has
 * left the graveyard by resolution is dropped (CR 608.2b); the rest return together.
 */
val MarchFromTheTomb = card("March from the Tomb") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Return any number of target Ally creature cards with total mana value 8 or less from your graveyard to the battlefield."

    spell {
        targets(
            TargetFilter.CreatureInYourGraveyard.withSubtype(Subtype.ALLY),
            unlimited = true,
            totalAtMost = TotalCap.manaValue(DynamicAmounts.fixed(8)),
        )
        effect = Effects.Pipeline {
            val allies = gather(CardSource.ChosenTargets)
            move(allies, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Lake Hurwitz"
        flavorText = "They came from all walks of life, from every part of Zendikar, and they returned with one unifying cause."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/044dd138-fc8e-4a24-ad6d-2df71f94c0c0.jpg?1783938179"
    }
}
