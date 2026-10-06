package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Oathsworn Vampire
 * {1}{B}
 * Creature — Vampire Knight
 * 2/2
 * This creature enters tapped.
 * You may cast this card from your graveyard if you gained life this turn.
 */
val OathswornVampire = card("Oathsworn Vampire") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Knight"
    power = 2
    toughness = 2
    oracleText = "This creature enters tapped.\n" +
        "You may cast this card from your graveyard if you gained life this turn."

    // This creature enters tapped.
    replacementEffect(EntersTapped())

    // "You may cast this card from your graveyard if you gained life this turn."
    // Conditional cast-from-graveyard permission; normal costs and sorcery timing still apply.
    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            condition = Conditions.YouGainedLifeThisTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Greg Opalinski"
        flavorText = "\"My conquistadors are never slain, merely laid to rest.\"\n—Queen Miralda the Pious"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/405f82d6-2991-4958-a563-572f0d298346.jpg?1783935309"
        ruling("2018-01-19", "Oathsworn Vampire's last ability cares only whether you gained life in the turn, even if Oathsworn Vampire wasn't in your graveyard when that happened. It doesn't care how much you gained, whether you also lost life, or even whether you lost more life than you gained.")
        ruling("2018-01-19", "Casting Oathsworn Vampire from your graveyard follows the normal rules for casting that card. You must pay its costs, and you must follow all applicable timing rules.")
    }
}
