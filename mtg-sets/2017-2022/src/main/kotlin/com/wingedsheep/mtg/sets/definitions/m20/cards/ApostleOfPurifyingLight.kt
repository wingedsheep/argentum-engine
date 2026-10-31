package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Apostle of Purifying Light (M20 #6)
 * {1}{W}  Creature — Human Cleric  2/1
 *
 * Protection from black
 * {2}: Exile target card from a graveyard.
 */
val ApostleOfPurifyingLight = card("Apostle of Purifying Light") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "Protection from black (This creature can't be blocked, targeted, dealt damage, enchanted, or equipped by anything black.)\n" +
        "{2}: Exile target card from a graveyard."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.protectionFrom(Color.BLACK))

    activatedAbility {
        cost = Costs.Mana("{2}")
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(t, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Bastien L. Deharme"
        flavorText = "Waves of gloom crash in vain against his unyielding light."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62f69602-b5fd-46d6-8dae-d77df35e378c.jpg"
    }
}
