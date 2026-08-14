package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Groffskithur — Mirrodin #121 (canonical printing; the Salvat 2005 boxes are later reprints)
 * {5}{G} · Creature — Beast · 3/3
 *
 * Whenever this creature becomes blocked, you may return target card named Groffskithur from your
 * graveyard to your hand.
 *
 * `Triggers.BecomesBlocked` fires once per combat no matter how many creatures block, which is the
 * printed behaviour — the ability isn't the "becomes blocked by a creature" per-blocker variant.
 *
 * The target is a *card named Groffskithur* in your graveyard, not "another Groffskithur creature
 * card": the filter is `GameObjectFilter.Any.named(...)`, so it would also find a copy that had been
 * turned into a noncreature card. `optional = true` is the "you may" — for a targeted trigger it
 * lets the controller decline by choosing no targets, and the trigger simply does nothing when no
 * legal target exists (the usual case, since a second Groffskithur in the yard is uncommon).
 */
val Groffskithur = card("Groffskithur") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature becomes blocked, you may return target card named " +
        "Groffskithur from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        optional = true
        val card = target(
            "target card named Groffskithur from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Any.named("Groffskithur").ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.ReturnToHand(card)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "John Matson"
        flavorText = "It growls not to threaten, but to summon."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75e84098-c15c-40f4-9d8a-3fa5da26a268.jpg?1783944533"
    }
}
