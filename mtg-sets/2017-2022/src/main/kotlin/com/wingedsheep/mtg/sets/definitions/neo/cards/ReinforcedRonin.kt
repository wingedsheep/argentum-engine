package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reinforced Ronin — Kamigawa: Neon Dynasty #158 (canonical printing)
 * {R} · Artifact Creature — Human Samurai · 2/2
 *
 * Haste
 * At the beginning of your end step, return this creature to its owner's hand.
 * Channel — {1}{R}, Discard this card: Draw a card.
 */
val ReinforcedRonin = card("Reinforced Ronin") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Human Samurai"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "At the beginning of your end step, return this creature to its owner's hand.\n" +
        "Channel — {1}{R}, Discard this card: Draw a card."

    keywords(Keyword.HASTE)

    // "Your end step" — only bounces at the end of its controller's turn.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    // Channel — {1}{R}, Discard this card (from hand): draw a card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.DrawCards(1)
        description = "Channel — {1}{R}, Discard this card: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89a18961-8f0b-4e98-9de4-aba97e91ad9a.jpg?1783923861"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}
