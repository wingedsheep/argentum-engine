package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Llanowar Loamspeaker
 * {1}{G}
 * Creature — Elf Druid
 * 1/3
 * {T}: Add one mana of any color.
 * {T}: Target land you control becomes a 3/3 Elemental creature with haste until end of turn. It's still a land. Activate only as a sorcery.
 */
val LlanowarLoamspeaker = card("Llanowar Loamspeaker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 3
    oracleText = "{T}: Add one mana of any color.\n{T}: Target land you control becomes a 3/3 Elemental creature with haste until end of turn. It's still a land. Activate only as a sorcery."

    // {T}: Add one mana of any color
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        description = "{T}: Add one mana of any color"
    }

    // {T}: Target land you control becomes a 3/3 Elemental creature with haste until end of turn. It's still a land.
    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        val land = target("land", TargetPermanent(filter = TargetFilter(GameObjectFilter.Land.youControl())))
        effect = Effects.BecomeCreature(
            target = land,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Zara Alfonso"
        flavorText = "Twigs broken underfoot in Llanowar tend to return the favor."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd5611db-82dd-464d-8b03-70d7619dcefe.jpg?1673307732"

        ruling("2022-09-09", "Llanowar Loamspeaker's second ability doesn't untap the land that becomes a creature.")
    }
}
