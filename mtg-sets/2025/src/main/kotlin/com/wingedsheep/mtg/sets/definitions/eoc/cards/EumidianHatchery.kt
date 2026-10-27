package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eumidian Hatchery — Edge of Eternities Commander #20 (canonical printing)
 * Land
 *
 * {T}, Pay 1 life: Add {B}. Put a hatchling counter on this land.
 * When this land is put into a graveyard from the battlefield, for each hatchling counter on it,
 * create a 1/1 black Insect creature token with flying.
 *
 * Glittering Stockpile's mana-then-counter mana ability. The graveyard trigger counts the
 * hatchling counters from the land's last-known information (counters cease to exist on the zone
 * change, CR 122.2), read off the leaves-the-battlefield snapshot on the trigger context.
 */
val EumidianHatchery = card("Eumidian Hatchery") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land"
    oracleText = "{T}, Pay 1 life: Add {B}. Put a hatchling counter on this land.\n" +
        "When this land is put into a graveyard from the battlefield, for each hatchling counter on it, " +
        "create a 1/1 black Insect creature token with flying."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.BLACK) then
            Effects.AddCounters(CounterType.HATCHLING, 1, EffectTarget.Self)
        manaAbility = true
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownSourceCounters(CounterType.HATCHLING),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Insect"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/2/1/2172a126-2ea6-4b5c-84c2-7879c1982a3a.jpg?1783905793"
        )
        description = "When this land is put into a graveyard from the battlefield, for each hatchling counter " +
            "on it, create a 1/1 black Insect creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25b57aaf-04fa-463d-8516-40fccd24d6ed.jpg?1783906061"
    }
}
