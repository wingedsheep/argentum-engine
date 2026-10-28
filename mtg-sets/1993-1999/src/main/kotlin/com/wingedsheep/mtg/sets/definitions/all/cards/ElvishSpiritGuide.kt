package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Elvish Spirit Guide
 * {2}{G}
 * Creature — Elf Spirit
 * 2/2
 * Exile this card from your hand: Add {G}.
 *
 * A mana ability that functions from the hand (CR 605.1a: it doesn't target and could add mana).
 */
val ElvishSpiritGuide = card("Elvish Spirit Guide") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Spirit"
    oracleText = "Exile this card from your hand: Add {G}."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.ExileSelf
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
        activateFromZone = Zone.HAND
        description = "Exile this card from your hand: Add {G}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Julie Baroh"
        flavorText = "\"We are never without guidance, if we but seek it.\"\n—Taaveti of Kelsinko, Elvish Hunter"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b94f37f-ebdf-4b79-a615-58331d27cf4e.jpg?1783947172"
    }
}
