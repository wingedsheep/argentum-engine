package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Photosynthetic Physician
 * {4}{G}
 * Creature — Plant Doctor
 * 5/5
 *
 * Reach
 * {G}, Exile this card from your graveyard: Create a 1/1 red Officer creature token.
 * Activate only as a sorcery.
 */
val PhotosyntheticPhysician = card("Photosynthetic Physician") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Doctor"
    power = 5
    toughness = 5
    oracleText = "Reach\n{G}, Exile this card from your graveyard: Create a 1/1 red Officer creature token. " +
        "Activate only as a sorcery."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        )
        description = "{G}, Exile this card from your graveyard: Create a 1/1 red Officer creature token. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Stella Spente"
        flavorText = "Despite the Phylosians' advanced medical knowledge, an invasive bacteria nearly " +
            "destroyed all life on their homeworld."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88b87ddb-fbf8-4e6e-9895-1b30ae5ad63a.jpg?1793361367"
    }
}
