package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clan Guildmage — Ravnica Allegiance #162
 * {R}{G} · Creature — Human Shaman · 2 / 2
 *
 * The Gruul entry in the RNA Guildmage cycle. The animated land keeps its land types and
 * abilities ("It's still a land") and gains haste, so it can attack or tap for mana the turn
 * it wakes up.
 */
val ClanGuildmage = card("Clan Guildmage") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "{1}{R}, {T}: Target creature can't block this turn.\n" +
        "{2}{G}, {T}: Target land you control becomes a 4/4 Elemental creature with haste until end of turn. It's still a land."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.BecomeCreature(
            target = land,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34a18415-015c-4d3f-8042-b156a673e125.jpg?1783933655"
    }
}
