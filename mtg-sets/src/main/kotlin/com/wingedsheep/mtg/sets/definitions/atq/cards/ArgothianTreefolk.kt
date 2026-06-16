package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Argothian Treefolk
 * {3}{G}{G}
 * Creature — Treefolk
 * 3/5
 * Prevent all damage that would be dealt to this creature by artifact sources.
 *
 * A single continuous [PreventDamage] replacement (CR 615) with recipient =
 * [RecipientFilter.Self] (only Argothian Treefolk) and source = any artifact.
 * Like Argothian Pixies' damage-prevention static, but with no block restriction
 * and matching all artifact sources rather than just artifact creatures.
 */
val ArgothianTreefolk = card("Argothian Treefolk") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 3
    toughness = 5
    oracleText = "Prevent all damage that would be dealt to this creature by artifact sources."

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Self,
                source = SourceFilter.Matching(GameObjectFilter.Artifact)
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Amy Weber"
        flavorText = "Haunting cries we hear in our dreams As the forest dies, a death from machines."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8db8882e-4db6-4e3c-9e9e-8c71d557a071.jpg?1562924902"
    }
}
