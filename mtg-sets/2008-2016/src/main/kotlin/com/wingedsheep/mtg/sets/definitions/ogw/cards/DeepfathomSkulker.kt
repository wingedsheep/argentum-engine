package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deepfathom Skulker
 * {5}{U}
 * Creature — Eldrazi
 * 4/4
 * Devoid (This card has no color.)
 * Whenever a creature you control deals combat damage to a player, you may draw a card.
 * {3}{C}: Target creature can't be blocked this turn. ({C} represents colorless mana.)
 *
 * The draw is Skulker's own triggered ability watching each creature you control (Starwinder's
 * shape), not an ability granted to those creatures: a creature that has lost all abilities still
 * makes Skulker trigger.
 */
val DeepfathomSkulker = card("Deepfathom Skulker") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi"
    power = 4
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Whenever a creature you control deals combat damage to a player, you may draw a card.\n" +
        "{3}{C}: Target creature can't be blocked this turn. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Effects.DrawCards(1))
    }

    activatedAbility {
        cost = Costs.Mana("{3}{C}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/496a22cc-36dd-4d4d-b887-8c4733a29870.jpg"
    }
}
