package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rootrunner
 * {2}{G}{G}
 * Creature — Spirit
 * 3/3
 * {G}{G}, Sacrifice this creature: Put target land on top of its owner's library.
 * Soulshift 3 (When this creature dies, you may return target Spirit card with mana value 3 or less
 * from your graveyard to your hand.)
 *
 * Sacrificing Rootrunner to its own ability is a death, so soulshift triggers off the cost.
 */
val Rootrunner = card("Rootrunner") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "{G}{G}, Sacrifice this creature: Put target land on top of its owner's library.\n" +
        "Soulshift 3 (When this creature dies, you may return target Spirit card with mana value 3 or less " +
        "from your graveyard to your hand.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}"), Costs.SacrificeSelf)
        val land = target(TargetFilter.Land)
        effect = Effects.Move(land, Zone.LIBRARY, ZonePlacement.Top)
    }
    soulshift(3)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23e5502b-acae-4320-ac01-4d711cb2c49a.jpg?1783944283"
    }
}
