package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Alchemist's Refuge — Avacyn Restored #225
 * Land
 *
 * {T}: Add {C}.
 * {G}{U}, {T}: You may cast spells this turn as though they had flash.
 *
 * Emergence Zone's shape without the sacrifice: a turn-scoped [Effects.GrantFlashToSpells] on the
 * controller, matching every spell.
 */
val AlchemistsRefuge = card("Alchemist's Refuge") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{G}{U}, {T}: You may cast spells this turn as though they had flash."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{U}"), Costs.Tap)
        effect = Effects.GrantFlashToSpells(
            spellFilter = GameObjectFilter.Any,
            duration = Duration.EndOfTurn
        )
        description = "{G}{U}, {T}: You may cast spells this turn as though they had flash."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Dan Murayama Scott"
        flavorText = "Persecuted alchemists fled deep into Kessig's wilds, seeking solitude to continue " +
            "their ghoulish craft."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c767a897-52e3-4401-8104-930157bb2b02.jpg?1783940651"
    }
}
