package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * The Underworld Cookbook — Modern Horizons 2 #240
 * {1} · Artifact — Book
 *
 * {T}, Discard a card: Create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 * {4}, {T}, Sacrifice this artifact: Return target creature card from your graveyard to your hand.
 *
 * Asmoranomardicadaistinaculdacar's recipe book: a one-mana discard outlet that pays you in Food,
 * with a late-game escape hatch that trades the book itself for a creature back.
 *
 * Both halves are activated abilities. The discard is a *cost* ([Costs.DiscardCard] — any card in
 * hand, not this one), which is exactly why the card is played alongside madness and
 * "whenever you discard" payoffs. The Food itself is the predefined token facade
 * [Effects.CreateFood]; the reminder text in the Oracle line is the token's own printed ability,
 * so nothing here wires it.
 *
 * The second ability's cost order mirrors the printed line — mana, tap, sacrifice — and because
 * sacrificing is part of the cost the book is already in the graveyard when the ability resolves.
 * The target is a creature card in *your* graveyard ([TargetFilter.CreatureInYourGraveyard]),
 * chosen on announcement, so it is picked before the book joins it there.
 */
val TheUnderworldCookbook = card("The Underworld Cookbook") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Book"
    oracleText = "{T}, Discard a card: Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{4}, {T}, Sacrifice this artifact: Return target creature card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.DiscardCard)
        effect = Effects.CreateFood()
        description = "{T}, Discard a card: Create a Food token."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target("target creature card from your graveyard", TargetObject(filter = TargetFilter.CreatureInYourGraveyard))
        effect = Effects.Move(t, Zone.HAND)
        description = "{4}, {T}, Sacrifice this artifact: Return target creature card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "240"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/039d62b0-3309-4424-a2ea-5a0d88d4bd72.jpg?1783926799"
    }
}
