package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mirrormade
 * {1}{U}{U}
 * Enchantment
 *
 * You may have this enchantment enter as a copy of any artifact or enchantment on the battlefield.
 *
 * Copy Enchantment's [EntersAsCopy] replacement with the copy filter widened to artifact or
 * enchantment (Copy Artifact's half joined in).
 */
val Mirrormade = card("Mirrormade") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "You may have this enchantment enter as a copy of any artifact or enchantment on the battlefield."

    replacementEffect(
        EntersAsCopy(optional = true, copyFilter = GameObjectFilter.Artifact or GameObjectFilter.Enchantment)
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Volkan Baǵa"
        flavorText = "\"Share your darkest secret and the Mirror will reveal your soul's essence.\"\n—Ceri, royal mirrorkeeper"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a10c1407-d397-4caa-b7b7-e7d91ffd4ee9.jpg?1783932654"
        ruling("2019-10-04", "If the chosen permanent is copying something else (for example, if the chosen permanent is another Mirrormade), then Mirrormade enters the battlefield as whatever the chosen permanent copied.")
        ruling("2019-10-04", "If Mirrormade somehow enters the battlefield at the same time as another artifact or enchantment, it can't become a copy of that permanent. You may choose only a permanent that's already on the battlefield.")
    }
}
