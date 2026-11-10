package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baloth Null — Oath of the Gatewatch #152 (canonical printing)
 * {4}{B}{G} · Creature — Zombie Beast · 4/5
 *
 * When this creature enters, return up to two target creature cards from your graveyard to your hand.
 *
 * Gravedigger's ETB widened to Dutiful Return's "up to two" target shape.
 */
val BalothNull = card("Baloth Null") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Zombie Beast"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, return up to two target creature cards from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.CreatureInYourGraveyard, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND),
        )
        description = "When this creature enters, return up to two target creature cards from your " +
            "graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Jakub Kasper"
        flavorText = "\"Humans use pigs to hunt for mushrooms, don't they? It's like that.\"\n—Harak, Malakir bloodwitch"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8811d210-23e2-4318-9730-7ee3b2021c68.jpg?1783937897"
    }
}
