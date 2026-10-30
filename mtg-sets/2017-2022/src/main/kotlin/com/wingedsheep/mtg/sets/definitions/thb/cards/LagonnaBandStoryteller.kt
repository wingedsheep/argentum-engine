package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lagonna-Band Storyteller
 * {3}{W}
 * Creature — Centaur Advisor
 * 3/4
 * When this creature enters, you may put target enchantment card from your graveyard on top of your
 * library. If you do, you gain life equal to its mana value.
 *
 * The "if you do" life gain sits inside the same [Effects.May] as the move, so declining gains
 * nothing; an illegal target fizzles the whole ability. The mana value is read off the moved card
 * (Divine Offering's shape), with {X} counting as 0 outside the stack (the ruling).
 */
val LagonnaBandStoryteller = card("Lagonna-Band Storyteller") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Centaur Advisor"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you may put target enchantment card from your graveyard on top " +
        "of your library. If you do, you gain life equal to its mana value."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.May(
            Effects.PutOnTopOfLibrary(enchantment) then
                Effects.GainLife(DynamicAmounts.manaValueOf(enchantment))
        )
        description = "When this creature enters, you may put target enchantment card from your graveyard on " +
            "top of your library. If you do, you gain life equal to its mana value."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Chris Seaman"
        flavorText = "\"Our stories are full of ancient wisdom; they tell us who we are.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3a87498-2c2c-411a-aa71-d594b2c6cb26.jpg?1783931593"
        ruling("2020-01-24", "If a card in a player's graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
