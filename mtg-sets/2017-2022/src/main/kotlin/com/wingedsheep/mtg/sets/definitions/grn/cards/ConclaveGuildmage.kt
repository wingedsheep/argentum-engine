package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Conclave Guildmage
 * {G}{W}
 * Creature — Elf Cleric
 * 2/2
 * {G}, {T}: Creatures you control gain trample until end of turn.
 * {5}{W}, {T}: Create a 2/2 green and white Elf Knight creature token with vigilance.
 */
val ConclaveGuildmage = card("Conclave Guildmage") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elf Cleric"
    oracleText = "{G}, {T}: Creatures you control gain trample until end of turn.\n" +
        "{5}{W}, {T}: Create a 2/2 green and white Elf Knight creature token with vigilance."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{W}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Elf", "Knight"),
            keywords = setOf(Keyword.VIGILANCE)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/279e551d-3ac3-48d5-9822-1d05159c8905.jpg?1783934139"
    }
}
