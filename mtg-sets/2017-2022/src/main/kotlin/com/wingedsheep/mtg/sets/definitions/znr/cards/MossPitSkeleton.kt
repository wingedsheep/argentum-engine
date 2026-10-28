package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moss-Pit Skeleton
 * {B}{G}
 * Creature — Plant Skeleton
 * 2/2
 *
 * Kicker {3}
 * If this creature was kicked, it enters with three +1/+1 counters on it.
 * Whenever one or more +1/+1 counters are put on a creature you control, if this card is in your
 * graveyard, you may put this card on top of your library.
 *
 * - The kicked-counter clause is a self-only [EntersWithCounters] replacement gated on [WasKicked].
 * - The recursion ability functions only from the graveyard (`triggerZone = GRAVEYARD`, CR 113.6b),
 *   so it can't fire while the Skeleton is on the battlefield. "One or more" is per creature: the
 *   non-batch `getsCounters` watcher merges a recipient's simultaneous placements into one trigger,
 *   so counters on two creatures at once trigger it twice, and a three-counter placement once.
 *   A creature entering with +1/+1 counters also counts (Scryfall ruling).
 * - The intervening-if [Conditions.SourceInZone] re-checks at resolution (CR 603.4): once the
 *   first of several triggers has put the card on top of the library, the rest do nothing, and the
 *   move reads `fromZone = GRAVEYARD` so a card that left the graveyard is never pulled back.
 */
val MossPitSkeleton = card("Moss-Pit Skeleton") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Plant Skeleton"
    power = 2
    toughness = 2
    oracleText = "Kicker {3} (You may pay an additional {3} as you cast this spell.)\n" +
        "If this creature was kicked, it enters with three +1/+1 counters on it.\n" +
        "Whenever one or more +1/+1 counters are put on a creature you control, if this card is in " +
        "your graveyard, you may put this card on top of your library."

    keywordAbility(KeywordAbility.kicker("{3}"))

    // If this creature was kicked, it enters with three +1/+1 counters on it.
    replacementEffect(
        EntersWithCounters(
            count = 3,
            selfOnly = true,
            condition = WasKicked,
        )
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.SourceInZone(Zone.GRAVEYARD)
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, ZonePlacement.Top, fromZone = Zone.GRAVEYARD)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95a445dc-3aea-4281-a145-0ab3a28ec6c6.jpg?1783929323"
        ruling(
            "2020-09-25",
            "Moss-Pit Skeleton's last ability triggers if a creature enters the battlefield under your " +
                "control with +1/+1 counters on it, as well as when +1/+1 counters are put on a creature " +
                "you already control."
        )
        ruling(
            "2020-09-25",
            "Moss-Pit Skeleton's last ability triggers only if it's already in your graveyard as +1/+1 " +
                "counters are put onto a creature you control."
        )
    }
}
