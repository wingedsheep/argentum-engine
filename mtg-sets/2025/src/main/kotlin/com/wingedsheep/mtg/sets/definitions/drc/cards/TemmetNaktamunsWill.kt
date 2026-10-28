package com.wingedsheep.mtg.sets.definitions.drc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temmet, Naktamun's Will — Aetherdrift Commander #4 (canonical printing)
 * {2}{W}{U}{B} · Legendary Creature — Zombie Wizard · 4/4
 *
 * Vigilance, menace
 * Whenever you attack, draw a card, then discard a card.
 * Whenever you draw a card, Zombies you control get +1/+1 until end of turn.
 *
 * The pump affects the Zombies you control as the ability resolves (CR 611.2c), so it iterates a
 * snapshot of that group rather than granting a floating group bonus.
 */
val TemmetNaktamunsWill = card("Temmet, Naktamun's Will") {
    manaCost = "{2}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Zombie Wizard"
    power = 4
    toughness = 4
    oracleText = "Vigilance, menace\n" +
        "Whenever you attack, draw a card, then discard a card.\n" +
        "Whenever you draw a card, Zombies you control get +1/+1 until end of turn."

    keywords(Keyword.VIGILANCE, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.DrawCards(1) then Effects.Discard(1)
        description = "Whenever you attack, draw a card, then discard a card."
    }

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl.withSubtype("Zombie"),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
        description = "Whenever you draw a card, Zombies you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Douglas Shuler"
        flavorText = "Each time he glimpsed oblivion, he learned another lesson."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0629894-0b72-4abe-9771-03becfdb241c.jpg?1783907753"
    }
}
