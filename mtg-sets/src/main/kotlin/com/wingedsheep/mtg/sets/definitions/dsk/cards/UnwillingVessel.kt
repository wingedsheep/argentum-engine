package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey

/**
 * Unwilling Vessel — Duskmourn: House of Horror #81
 * {2}{U} · Creature — Human Wizard · 3/2
 *
 * Vigilance
 * Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, put a
 * possession counter on this creature.
 * When this creature dies, create an X/X blue Spirit creature token with flying, where X is the
 * number of counters on this creature.
 *
 * The "Eerie" ability word is flavor; mechanically it is two triggered abilities (like the other
 * DSK Eerie creatures, e.g. Balemurk Leech): one on `Triggers.entersBattlefield` filtered to
 * enchantments you control, one on `Triggers.RoomFullyUnlocked`. Each adds one possession counter
 * (a passive storage counter, no inherent rule) to this creature. The dies trigger creates one
 * X/X token where X is read at death from `ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT` —
 * the total number of counters of every kind on the creature when it died (per the ruling, +1/+1
 * and -1/-1 counters are included, not just possession counters).
 */
val UnwillingVessel = card("Unwilling Vessel") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2
    oracleText = "Vigilance\n" +
        "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room, " +
        "put a possession counter on this creature.\n" +
        "When this creature dies, create an X/X blue Spirit creature token with flying, where X is " +
        "the number of counters on this creature."

    keywords(Keyword.VIGILANCE, Keyword.EERIE)

    // Eerie trigger — part 1: whenever an enchantment you control enters
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(Counters.POSSESSION, 1, EffectTarget.Self)
        description = "Eerie — Whenever an enchantment you control enters, put a possession " +
            "counter on this creature."
    }

    // Eerie trigger — part 2: whenever you fully unlock a Room
    triggeredAbility {
        trigger = Triggers.RoomFullyUnlocked
        effect = Effects.AddCounters(Counters.POSSESSION, 1, EffectTarget.Self)
        description = "Eerie — Whenever you fully unlock a Room, put a possession counter on this " +
            "creature."
    }

    // When this creature dies, create an X/X blue Spirit creature token with flying.
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT),
            dynamicToughness = DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT),
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/90f6d606-55ca-4431-ae61-5d4f05259403.jpg?1726236595",
        )
        description = "When this creature dies, create an X/X blue Spirit creature token with " +
            "flying, where X is the number of counters on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d1758ed-fe33-4ead-8c83-e54fabcb4cfe.jpg?1726286151"

        ruling("2024-09-20", "If a permanent with an eerie ability enters at the same time as one or more enchantments, its ability will trigger for each of those enchantments.")
        ruling("2024-09-20", "An ability that triggers \"whenever you fully unlock a Room\" triggers when a door becomes unlocked and the other door of that Room is already unlocked, or when both doors of that Room become unlocked simultaneously.")
        ruling("2024-09-20", "Use the total number of counters that were on Unwilling Vessel when it died, including all of the +1/+1 and -1/-1 counters, to determine the value of X.")
    }
}
