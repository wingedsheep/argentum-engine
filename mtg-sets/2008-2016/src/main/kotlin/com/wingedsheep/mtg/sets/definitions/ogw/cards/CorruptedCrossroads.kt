package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Corrupted Crossroads
 * Land
 * {T}: Add {C}. ({C} represents colorless mana.)
 * {T}, Pay 1 life: Add one mana of any color. Spend this mana only to cast a spell with devoid.
 *
 * The restriction keys on the devoid keyword, not on being colorless: a colorless artifact
 * spell without devoid can't spend it (ruling).
 */
val CorruptedCrossroads = card("Corrupted Crossroads") {
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}. ({C} represents colorless mana.)\n" +
        "{T}, Pay 1 life: Add one mana of any color. Spend this mana only to cast a spell with devoid."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddManaOfChoice(restriction = ManaRestriction.SpellsWithKeywordOnly(Keyword.DEVOID))
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Ryan Pancoast"
        flavorText = "\"Look, and see the future.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7043e193-8051-4f1b-9633-9c1f250d3607.jpg"
        ruling(
            "2016-01-22",
            "The mana produced by the last ability can't be spent on a colorless spell unless that spell specifically has the devoid ability."
        )
    }
}
