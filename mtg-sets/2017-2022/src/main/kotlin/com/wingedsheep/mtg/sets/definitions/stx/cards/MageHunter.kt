package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mage Hunter — Strixhaven: School of Mages #76 (canonical printing)
 * {3}{B} · Creature — Horror · 3/4
 *
 * Whenever an opponent casts or copies an instant or sorcery spell, they lose 1 life.
 *
 * Magecraft's trigger pointed at opponents: `Triggers.anOpponent.castsOrCopies(InstantOrSorcery)`.
 * Both the cast and the copy event set the triggering player (the caster / the copy's controller),
 * so "they" is [Player.TriggeringPlayer].
 */
val MageHunter = card("Mage Hunter") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 4
    oracleText = "Whenever an opponent casts or copies an instant or sorcery spell, they lose 1 life."

    triggeredAbility {
        trigger = Triggers.anOpponent.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent casts or copies an instant or sorcery spell, they lose 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Mathias Kollros"
        flavorText = "One of the Oriq's most powerful tools is the mage hunter, a vile monstrosity " +
            "created to seek out and snuff out the arcane."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56fcbe4a-2d98-4fa9-a6c3-e28255171a4d.jpg?1783927367"
        ruling("2021-04-16", "Mage Hunter's triggered ability will resolve before the spell that caused it to trigger.")
    }
}
