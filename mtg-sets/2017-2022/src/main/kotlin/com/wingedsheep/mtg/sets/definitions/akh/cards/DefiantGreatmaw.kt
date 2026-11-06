package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Defiant Greatmaw — Amonkhet #163 (canonical printing)
 * {2}{G} · Creature — Hippo · 4/5
 *
 * When this creature enters, put two -1/-1 counters on target creature you control.
 * Whenever you put one or more -1/-1 counters on this creature, remove a -1/-1 counter from another
 * target creature you control.
 *
 * The second ability is a SELF-bound `CountersPlacedEvent` scoped by `placedBy = You`, so counters an
 * opponent puts on it (or wither damage from an opponent's source, CR 120.3d) don't trigger it. It
 * triggers even when the counters kill it (ruling 2017-04-18).
 */
val DefiantGreatmaw = card("Defiant Greatmaw") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hippo"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, put two -1/-1 counters on target creature you control.\n" +
        "Whenever you put one or more -1/-1 counters on this creature, remove a -1/-1 counter from another " +
        "target creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 2, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.MINUS_ONE_MINUS_ONE, by = Player.You)
        val other = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, other)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b74cb0ba-c691-4823-aa94-399b1aaeaf0b.jpg?1783936477"
        ruling("2017-04-18", "If you put enough -1/-1 counters on Defiant Greatmaw so that its toughness is 0 or less, its last ability triggers.")
        ruling("2017-04-18", "If a creature with wither or infect deals damage to a creature, the controller of the creature with wither or infect puts that many -1/-1 counters on the second creature.")
    }
}
