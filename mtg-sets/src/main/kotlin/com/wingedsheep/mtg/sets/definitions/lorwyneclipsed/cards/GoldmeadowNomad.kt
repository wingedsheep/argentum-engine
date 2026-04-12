package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Goldmeadow Nomad
 * {W}
 * Creature — Kithkin Scout
 * 1/2
 *
 * {W}, Exile this card from your graveyard: Create a 1/1 green and white
 * Kithkin creature token. Activate only as a sorcery.
 */
val GoldmeadowNomad = card("Goldmeadow Nomad") {
    manaCost = "{W}"
    typeLine = "Creature — Kithkin Scout"
    power = 1
    toughness = 2
    oracleText = "{W}, Exile this card from your graveyard: Create a 1/1 green and white Kithkin creature token. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Paolo Parente"
        flavorText = "\"During my time on the road, the emptiness I felt after severing myself from the thoughtweft has faded with every story heard and every tale spread.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00ddbe6c-11de-4bc6-aabe-d6d8385a838a.jpg?1767871695"
    }
}
