package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Argothian Pixies
 * {1}{G}
 * Creature — Faerie
 * 2/1
 * This creature can't be blocked by artifact creatures.
 * Prevent all damage that would be dealt to this creature by artifact creatures.
 *
 * Two static abilities filtered to artifact creatures ([GameObjectFilter.ArtifactCreature]):
 *  - a [CantBeBlockedBy] blocking restriction, and
 *  - a continuous [PreventDamage] replacement (CR 615) with recipient = [RecipientFilter.Self]
 *    (only Argothian Pixies) and source = any artifact creature.
 */
val ArgothianPixies = card("Argothian Pixies") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Faerie"
    power = 2
    toughness = 1
    oracleText = "This creature can't be blocked by artifact creatures.\n" +
        "Prevent all damage that would be dealt to this creature by artifact creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.ArtifactCreature)
    }

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Self,
                source = SourceFilter.Matching(GameObjectFilter.ArtifactCreature)
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Amy Weber"
        flavorText = "After the rape of Argoth Forest during the rule of the artificers, the Pixies of Argoth bent their magic to more practical ends."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5712e87a-2381-4f5b-a853-6973841f9bf1.jpg?1562913202"
    }
}
