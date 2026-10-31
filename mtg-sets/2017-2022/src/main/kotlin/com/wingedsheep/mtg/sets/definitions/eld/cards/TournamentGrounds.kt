package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Tournament Grounds
 * Land
 * {T}: Add {C}.
 * {T}: Add {R}, {W}, or {B}. Spend this mana only to cast a Knight or Equipment spell.
 *
 * The three-colour choice is one restricted mana ability per colour (as the Tainted lands split
 * theirs), each carrying `SubtypeSpellsOnly(Knight, Equipment)`.
 */
val TournamentGrounds = card("Tournament Grounds") {
    manaCost = ""
    colorIdentity = "BRW"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add {R}, {W}, or {B}. Spend this mana only to cast a Knight or Equipment spell."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    for (color in listOf(Color.RED, Color.WHITE, Color.BLACK)) {
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(
                color,
                restriction = ManaRestriction.SubtypeSpellsOnly(setOf("Knight", "Equipment")),
            )
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Cristi Balanescu"
        flavorText = "No one knows when the tournament began. All they know is that it will never end."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd438d51-a778-4b38-8b4b-a6a9cd9b4b22.jpg"
    }
}
