package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities

/**
 * Stony Silence
 * {1}{W}
 * Enchantment
 * Activated abilities of artifacts can't be activated.
 *
 * Modeling notes:
 * - The Cursed Totem shape over artifacts: [PreventActivatedAbilities]`(GameObjectFilter.Artifact)`.
 *   The default scope covers every activated ability, mana abilities included, and the default
 *   battlefield-only reach leaves abilities that work from other zones (cycling) alone, per the rulings.
 */
val StonySilence = card("Stony Silence") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Activated abilities of artifacts can't be activated."

    staticAbility {
        ability = PreventActivatedAbilities(GameObjectFilter.Artifact)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Wayne England"
        flavorText = "Let moss grow over gargoyles\n—Gavony saying meaning \"forget painful memories\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f56a5a73-5f10-4f97-989f-7cea0a8d95e3.jpg?1783940984"
        ruling("2017-03-14", "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keyword abilities, such as equip, are activated abilities and will have colons in their reminder text.")
        ruling("2017-03-14", "No abilities of artifacts can be activated, including mana abilities.")
        ruling("2017-03-14", "Stony Silence's ability affects only artifacts on the battlefield. Activated abilities that work in other zones (such as cycling) can still be activated. Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected.")
    }
}
