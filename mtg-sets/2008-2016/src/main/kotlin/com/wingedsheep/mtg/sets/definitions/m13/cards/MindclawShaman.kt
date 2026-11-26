package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Mindclaw Shaman — Magic 2013 #142 (canonical printing)
 * {4}{R} · Creature — Lizard Shaman · 2/2
 *
 * When this creature enters, target opponent reveals their hand. You may cast an instant or sorcery
 * spell from among those cards without paying its mana cost.
 *
 * Silent-Blade Oni's pipeline on a targeted ETB: the reveal is public, the cast is an up-to-one
 * choice among the opponent's instant and sorcery cards, cast during resolution.
 */
val MindclawShaman = card("Mindclaw Shaman") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Shaman"
    oracleText = "When this creature enters, target opponent reveals their hand. You may cast an instant or " +
        "sorcery spell from among those cards without paying its mana cost."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val theirHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val spellToCast = chooseUpTo(
                1,
                from = theirHand,
                filter = GameObjectFilter.InstantOrSorcery,
                showAllCards = true,
                prompt = "You may cast an instant or sorcery from their hand without paying its mana cost",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spellToCast))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Slawomir Maniak"
        flavorText = "Nicol Bolas gives power to those who will abuse it most."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f342fe9-aa73-4222-b908-d4035b5746be.jpg?1783940480"
        ruling("2012-07-01", "Any player can respond to Mindclaw Shaman's triggered ability. However, once that ability starts resolving and the target opponent reveals their hand, players can't respond until after the ability finishes resolving. For example, the opponent can't cast the instant or sorcery card you choose to cast in order to stop you from casting it.")
        ruling("2012-07-01", "You cast the card from the opponent's hand immediately. Ignore timing restrictions based on the card's type. Other timing restrictions, such as \"Cast [this card] only during combat,\" must be followed.")
        ruling("2012-07-01", "The card goes to its owner's graveyard after it resolves (or otherwise leaves the stack), not yours.")
        ruling("2012-07-01", "If you can't cast any instant or sorcery card (perhaps because there are no legal targets available) or if you choose not to cast one, then Mindclaw Shaman's ability finishes resolving and the game moves on.")
        ruling("2012-07-01", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can pay additional costs such as kicker costs. If the card has mandatory additional costs, you must pay those.")
        ruling("2012-07-01", "If the card has X in its mana cost, you must choose 0 as its value.")
    }
}
