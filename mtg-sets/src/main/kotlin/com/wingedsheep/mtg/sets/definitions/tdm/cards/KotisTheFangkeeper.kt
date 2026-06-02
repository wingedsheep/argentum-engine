package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Kotis, the Fangkeeper — Tarkir: Dragonstorm #202
 * {1}{B}{G}{U} · Legendary Creature — Zombie Warrior · 2/1
 *
 * Indestructible
 * Whenever Kotis deals combat damage to a player, exile the top X cards of their
 * library, where X is the amount of damage dealt. You may cast any number of spells
 * with mana value X or less from among them without paying their mana costs.
 *
 * This is the "exile top X, free-cast the spells with mana value ≤ X" shape, driven by a
 * combat-damage trigger. It's a resolution-time chain over existing pipeline primitives:
 *   1. gather the top X of the damaged player's library, where X is the combat damage
 *      read from the trigger context ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT]),
 *   2. exile that whole batch ([Player.TriggeringPlayer] is the damaged player),
 *   3. keep only nonland cards (you may cast *spells*, not play lands),
 *   4. of those, keep only mana value ≤ X (the same X, re-read from the context — the
 *      dynamic cap comes straight from [CollectionFilter.ManaValueAtMost] over a
 *      [DynamicAmount], no bespoke filter),
 *   5. cast any number of that filtered set for free, *during resolution*.
 *
 * Step 5 uses [CastAnyNumberFromCollectionWithoutPayingCostEffect]: the controller is
 * offered the eligible cards one at a time and may stop whenever they like. Per the official
 * rulings the casts happen while this ability is resolving (the controller can't wait until
 * later in the turn) and type-based timing is ignored — both fall out of casting through the
 * synthesized-cast path, exactly as Cascade does. Cards left uncast stay in exile.
 */
val KotisTheFangkeeper = card("Kotis, the Fangkeeper") {
    manaCost = "{1}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Zombie Warrior"
    power = 2
    toughness = 1
    oracleText = "Indestructible\n" +
        "Whenever Kotis deals combat damage to a player, exile the top X cards of their " +
        "library, where X is the amount of damage dealt. You may cast any number of spells " +
        "with mana value X or less from among them without paying their mana costs."

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        description = "Whenever Kotis deals combat damage to a player, exile the top X cards " +
            "of their library, where X is the amount of damage dealt. You may cast any number " +
            "of spells with mana value X or less from among them without paying their mana costs."

        effect = Effects.Composite(
            listOf(
                // Exile the top X cards of the damaged player's library (X = combat damage).
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
                        player = Player.TriggeringPlayer
                    ),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, player = Player.TriggeringPlayer)
                ),
                // Narrow to spells (nonland cards) with mana value ≤ X — the free-castable set.
                FilterCollectionEffect(
                    from = "exiled",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Nonland),
                    storeMatching = "nonland"
                ),
                FilterCollectionEffect(
                    from = "nonland",
                    filter = CollectionFilter.ManaValueAtMost(
                        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
                    ),
                    storeMatching = "castable"
                ),
                // Cast any number of them for free, during this ability's resolution.
                CastAnyNumberFromCollectionWithoutPayingCostEffect("castable")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3736f17-f80b-4b2c-b919-2c963bc14682.jpg?1743204793"
        ruling("2025-04-04", "If an exiled card has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-04-04", "You cast the spells from among the exiled cards while Kotis's last ability is resolving and still on the stack. You can't wait to cast them later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2025-04-04", "Since you are using an alternative cost to cast the spells, you can't pay any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the spells have any mandatory additional costs, you must pay those.")
        ruling("2025-04-04", "If any of the spells you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
