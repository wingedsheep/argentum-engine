package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllEffect
import com.wingedsheep.sdk.scripting.GroupFilter

/**
 * Armageddon
 * {3}{W}
 * Sorcery
 * Destroy all lands.
 */
val Armageddon = card("Armageddon") {
    manaCost = "{3}{W}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllEffect(GroupFilter.AllLands)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "John Avon"
        flavorText = "A world miserable as ours . . . is to be destroyed."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2073ca8b-2bca-4539-94d7-989da157e4b8.jpg"
    }
}
