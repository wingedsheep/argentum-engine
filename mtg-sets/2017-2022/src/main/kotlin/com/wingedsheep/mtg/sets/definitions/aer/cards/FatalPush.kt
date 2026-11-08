package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fatal Push — Aether Revolt #57
 * {B} · Instant
 *
 * Destroy target creature if it has mana value 2 or less.
 * Revolt — Destroy that creature if it has mana value 4 or less instead if a permanent left the
 * battlefield under your control this turn.
 *
 * The spell targets any creature; the mana value is only checked as it resolves (2020-08-07
 * ruling). Both clauses end in the same destroy, so the card collapses to one resolution-time
 * gate: destroy if the target's mana value is 2 or less, **or** revolt is on and its mana value
 * is 4 or less. Revolt reads the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker, as on Decommission.
 */
val FatalPush = card("Fatal Push") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature if it has mana value 2 or less.\n" +
        "Revolt — Destroy that creature if it has mana value 4 or less instead if a permanent left the " +
        "battlefield under your control this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.Any(
                Conditions.TargetMatchesFilter(GameObjectFilter.Any.manaValueAtMost(2), creature),
                Conditions.All(
                    Conditions.YouHadPermanentLeaveBattlefieldThisTurn,
                    Conditions.TargetMatchesFilter(GameObjectFilter.Any.manaValueAtMost(4), creature),
                ),
            ),
            then = Effects.Destroy(creature),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5e81649-9954-424c-89d1-f87d73b66047.jpg?1783936764"
        ruling("2020-08-07", "Fatal Push can target any creature, even one with mana value 5 or greater. The creature's mana value is checked only as Fatal Push resolves.")
        ruling("2020-08-07", "If a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-08-07", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Cast Down, or an enchantment you returned to your hand with Cyclonic Rift.")
        ruling("2020-08-07", "Tokens that leave the battlefield will satisfy a revolt ability.")
    }
}
