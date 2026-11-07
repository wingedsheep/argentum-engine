package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ajani's Aid — Aether Revolt #188 (planeswalker deck)
 * {2}{G}{W} · Enchantment
 *
 * When this enchantment enters, you may search your library and/or graveyard for a card named
 * Ajani, Valiant Protector, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 * Sacrifice this enchantment: Prevent all combat damage a creature of your choice would deal this turn.
 *
 * The prevention has no recipient clause, so it is a combat-only silence shield on the chosen
 * creature (`FromTarget` over a `Chosen` source), the combat-only cousin of Burrenton Forge-Tender's.
 * The creature is chosen as the ability resolves — it doesn't target — and only a creature on the
 * battlefield can be chosen. Its noncombat damage is still dealt.
 */
val AjanisAid = card("Ajani's Aid") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you may search your library and/or graveyard for a card " +
        "named Ajani, Valiant Protector, reveal it, and put it into your hand. If you search your library " +
        "this way, shuffle.\n" +
        "Sacrifice this enchantment: Prevent all combat damage a creature of your choice would deal this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Ajani, Valiant Protector"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this enchantment enters, you may search your library and/or graveyard for a " +
            "card named Ajani, Valiant Protector, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.PreventDamage(
            direction = PreventionDirection.FromTarget,
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Creature.onBattlefield()),
            combatOnly = true
        )
        description = "Sacrifice this enchantment: Prevent all combat damage a creature of your choice " +
            "would deal this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        inBooster = false
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7026c374-0776-403b-86fd-5092677fd5c9.jpg?1783936714"
        ruling(
            "2017-02-09",
            "The last ability of Ajani's Aid doesn't target anything. You choose a source of damage as " +
                "the ability resolves."
        )
        ruling(
            "2017-02-09",
            "You can activate the last ability even if there is no creature about to deal combat damage, " +
                "or even no creature at all. No damage will be prevented, but it will enable your revolt abilities."
        )
    }
}
