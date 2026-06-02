package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Breaching Dragonstorm — Tarkir: Dragonstorm #101
 * {4}{R} · Enchantment · Uncommon
 *
 * When this enchantment enters, exile cards from the top of your library until you exile a
 * nonland card. You may cast it without paying its mana cost if that spell's mana value is 8
 * or less. If you don't, put that card into your hand.
 * When a Dragon you control enters, return this enchantment to its owner's hand.
 *
 * The ETB is the "impulse-until-nonland, free-cast-if-cheap-else-to-hand" shape, composed from
 * existing pipeline primitives:
 *   1. [GatherUntilMatchEffect] exiles from the top until a [GameObjectFilter.Nonland] card —
 *      the nonland stored as `nonland`, every revealed card (lands + the nonland) as `allRevealed`,
 *   2. move `allRevealed` to exile,
 *   3. narrow the nonland to mana value ≤ 8 ([CollectionFilter.ManaValueAtMost]) → `castable`,
 *   4. `MayEffect(CastFromCollectionWithoutPayingCost("castable"))` — you may cast it for free
 *      (the may-cast simply has no candidate when MV > 8, so it's skipped),
 *   5. of the nonland, keep only the copy still in exile ([CollectionFilter.InZone] — the cast
 *      one has moved to the stack) → `uncast`, and put it into hand.
 * The lands exiled along the way stay in exile (only the nonland is ever moved to hand), matching
 * the printed card. The Dragon-bounce is the shared cycle trigger: [Triggers.entersBattlefield]
 * scoped to Dragons you control ([TriggerBinding.OTHER]) returning this enchantment to hand.
 */
val BreachingDragonstorm = card("Breaching Dragonstorm") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile cards from the top of your library until " +
        "you exile a nonland card. You may cast it without paying its mana cost if that spell's " +
        "mana value is 8 or less. If you don't, put that card into your hand.\n" +
        "When a Dragon you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                // Exile from the top of the library until a nonland card is exiled.
                GatherUntilMatchEffect(
                    filter = GameObjectFilter.Nonland,
                    storeMatch = "nonland",
                    storeRevealed = "allRevealed"
                ),
                MoveCollectionEffect(
                    from = "allRevealed",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // Only mana value ≤ 8 may be cast for free.
                FilterCollectionEffect(
                    from = "nonland",
                    filter = CollectionFilter.ManaValueAtMost(DynamicAmount.Fixed(8)),
                    storeMatching = "castable"
                ),
                // You may cast it without paying its mana cost — only prompted when there is a
                // mana-value-≤-8 nonland to cast (no empty "may cast" when MV > 8).
                ConditionalOnCollectionEffect(
                    collection = "castable",
                    ifNotEmpty = MayEffect(Effects.CastFromCollectionWithoutPayingCost("castable"))
                ),
                // If you don't (declined, or MV > 8), put that card into your hand. The card just
                // cast has left exile for the stack, so only the nonland still in exile moves.
                FilterCollectionEffect(
                    from = "nonland",
                    filter = CollectionFilter.InZone(Zone.EXILE),
                    storeMatching = "uncast"
                ),
                MoveCollectionEffect(
                    from = "uncast",
                    destination = CardDestination.ToZone(Zone.HAND)
                )
            )
        )
        description = "When this enchantment enters, exile cards from the top of your library " +
            "until you exile a nonland card. You may cast it without paying its mana cost if " +
            "that spell's mana value is 8 or less. If you don't, put that card into your hand."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "When a Dragon you control enters, return this enchantment to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Danny Schwartz"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2c2a069-7553-4879-abfb-b2aa3349e4b8.jpg?1743204368"
    }
}
