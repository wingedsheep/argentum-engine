package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Galactic Wayfarer
 * {2}{G}
 * Creature — Human Scout
 * When this creature enters, create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 * 3/3
 */
val GalacticWayfarer = card("Galactic Wayfarer") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    // ETB: create a Lander token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateLander()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Quintin Gleim"
        flavorText = "Most say the Edge cannot be measured, but some will spend their lives trying anyway."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85b898d2-050f-49a2-87af-07d54d105336.jpg?1752947308"
    }
}
