package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sage of the Falls
 * {4}{U}
 * Creature — Merfolk Wizard
 * 2/5
 *
 * Whenever this creature or another non-Human creature you control enters, you may draw a card.
 * If you do, discard a card.
 *
 * The Sage is printed as a non-Human creature, so "this creature or another non-Human creature
 * you control" reads as "a non-Human creature you control" — one [Triggers.a] enters trigger covers
 * both (it would miss the Sage only if an effect made it a Human). Each simultaneous arrival triggers separately and resolves its own draw-then-discard.
 */
val SageOfTheFalls = card("Sage of the Falls") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 5
    oracleText = "Whenever this creature or another non-Human creature you control enters, you may draw a card. " +
        "If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().notSubtype(Subtype.HUMAN)).enters()
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Nicholas Gregory"
        flavorText = "Ask a merfolk even a simple question and the answer is a journey far beyond the known."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/274e3aa4-4b46-4daa-a7a8-400a20c59435.jpg?1783932649"
        ruling(
            "2019-10-04",
            "You draw a card and discard a card all while Sage of the Falls's ability is resolving. " +
                "Nothing can happen between the two, and no player may choose to take actions.",
        )
        ruling(
            "2019-10-04",
            "If more than one non-Human creature enters the battlefield at the same time, Sage of the Falls's " +
                "ability triggers that many times. You'll resolve the abilities one at a time—you won't draw " +
                "that many cards at once and then discard that many cards.",
        )
    }
}
