package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spellkeeper Weird
 * {2}{U}
 * Creature — Weird
 * 1/4
 * {2}, {T}, Sacrifice this creature: Return target instant or sorcery card from your graveyard to your hand.
 */
val SpellkeeperWeird = card("Spellkeeper Weird") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Weird"
    power = 1
    toughness = 4
    oracleText = "{2}, {T}, Sacrifice this creature: Return target instant or sorcery card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.ReturnToHand(spellCard)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "James Paick"
        flavorText = "They worked well as a team, but they knew it couldn't last forever."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74e42dad-5958-443f-a196-12b3f3e44213.jpg?1783933456"
    }
}
