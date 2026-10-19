package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Colossal Skyturtle — Kamigawa: Neon Dynasty #216 (canonical printing)
 * {4}{G}{G}{U} · Enchantment Creature — Turtle · 6/5
 *
 * Flying, ward {2}
 * Channel — {2}{G}, Discard this card: Return target card from your graveyard to your hand.
 * Channel — {1}{U}, Discard this card: Return target creature to its owner's hand.
 *
 * Targets are chosen before the discard cost is paid, so the graveyard channel can't return the
 * Skyturtle itself.
 */
val ColossalSkyturtle = card("Colossal Skyturtle") {
    manaCost = "{4}{G}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment Creature — Turtle"
    power = 6
    toughness = 5
    oracleText = "Flying, ward {2}\n" +
        "Channel — {2}{G}, Discard this card: Return target card from your graveyard to your hand.\n" +
        "Channel — {1}{U}, Discard this card: Return target creature to its owner's hand."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    // Channel — {2}{G}, Discard this card (from hand): regrow a card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.Move(t, Zone.HAND)
        description = "Channel — {2}{G}, Discard this card: Return target card from your graveyard to your hand."
    }

    // Channel — {1}{U}, Discard this card (from hand): bounce a creature.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(t)
        description = "Channel — {1}{U}, Discard this card: Return target creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f40bd797-4d12-4098-a1a8-d7e5b7b82ac9.jpg?1783923837"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
