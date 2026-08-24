package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Unbreathing Horde
 * {2}{B}
 * Creature — Zombie
 * 0/0
 * This creature enters with a +1/+1 counter on it for each other Zombie you control and each
 * Zombie card in your graveyard.
 * If this creature would be dealt damage, prevent that damage and remove a +1/+1 counter from it.
 *
 * The entry count is one [DynamicAmount.Add] over the two zones the printed line names. The
 * battlefield half is `excludeSelf` for "each **other** Zombie" — the Horde is on its way in, but
 * the count is evaluated against a state that already holds it, so the exclusion is load-bearing
 * rather than decorative.
 *
 * The graveyard half is deliberately *not* excluded, and that is the printed ruling: reanimate the
 * Horde and it counts itself, because it is still in the graveyard as its own enters-with
 * replacement is applied.
 *
 * The damage clause is [PreventDamageByRemovingCounter] — the printed twin of the shield counter
 * (CR 122.1c), which is why it spends exactly one counter per damage event no matter how large the
 * damage, and why it still prevents once the counters are gone.
 */
val UnbreathingHorde = card("Unbreathing Horde") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "This creature enters with a +1/+1 counter on it for each other Zombie you " +
        "control and each Zombie card in your graveyard.\n" +
        "If this creature would be dealt damage, prevent that damage and remove a +1/+1 counter from it."
    power = 0
    toughness = 0

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.Add(
                DynamicAmount.Count(
                    player = Player.You,
                    zone = Zone.BATTLEFIELD,
                    filter = GameObjectFilter.Creature
                        .withSubtype(Subtype.ZOMBIE)
                        .notSourceItself()
                ),
                DynamicAmount.Count(
                    player = Player.You,
                    zone = Zone.GRAVEYARD,
                    filter = GameObjectFilter.Any.withSubtype(Subtype.ZOMBIE)
                )
            )
        )
    )

    replacementEffect(PreventDamageByRemovingCounter())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a91ea47-0c06-4333-a309-ac360c5cc9bd.jpg?1783940947"
        ruling("2011-09-22", "Only one +1/+1 counter will be removed, no matter how much damage is prevented.")
        ruling("2011-09-22", "If Unbreathing Horde has no +1/+1 counters on it (but its toughness is raised above 0 by another effect), any damage dealt to it will still be prevented, even though no counter will be removed.")
        ruling("2011-09-22", "If Unbreathing Horde enters from a graveyard, it will count itself when determining how many +1/+1 counters it enters with.")
    }
}
