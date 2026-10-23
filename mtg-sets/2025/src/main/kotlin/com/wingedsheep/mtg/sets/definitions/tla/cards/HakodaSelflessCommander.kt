package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hakoda, Selfless Commander
 * {3}{W}
 * Legendary Creature — Human Warrior Ally
 * 3/5
 * Vigilance
 * You may look at the top card of your library any time.
 * You may cast Ally spells from the top of your library.
 * Sacrifice Hakoda: Creatures you control get +0/+5 and gain indestructible until end of turn.
 */
val HakodaSelflessCommander = card("Hakoda, Selfless Commander") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior Ally"
    power = 3
    toughness = 5
    oracleText = "Vigilance\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast Ally spells from the top of your library.\n" +
        "Sacrifice Hakoda: Creatures you control get +0/+5 and gain indestructible until end of turn."

    keywords(Keyword.VIGILANCE)

    // You may look at the top card of your library any time.
    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    // You may cast Ally spells from the top of your library.
    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.ALLY)
        )
    }

    // Sacrifice Hakoda: Creatures you control get +0/+5 and gain indestructible until end of turn.
    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(0, 5, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9aef3ddb-9bb7-42c8-975b-b1917955a416.jpg?1764120030"
    }
}
