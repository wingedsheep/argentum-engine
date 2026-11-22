package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Melek, Izzet Paragon
 * {4}{U}{R}
 * Legendary Creature — Weird Wizard
 * 2/4
 * Play with the top card of your library revealed.
 * You may cast instant and sorcery spells from the top of your library.
 * Whenever you cast an instant or sorcery spell from your library, copy it. You may choose new
 * targets for the copy.
 */
val MelekIzzetParagon = card("Melek, Izzet Paragon") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Weird Wizard"
    power = 2
    toughness = 4
    oracleText = "Play with the top card of your library revealed.\n" +
        "You may cast instant and sorcery spells from the top of your library.\n" +
        "Whenever you cast an instant or sorcery spell from your library, copy it. " +
        "You may choose new targets for the copy."

    staticAbility {
        ability = RevealTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.InstantOrSorcery)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.LIBRARY)),
        )
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e892d86-f443-4846-8049-40ec6b8c22b4.jpg?1783940025"
    }
}
