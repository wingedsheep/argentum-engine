package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undercity Scavenger — Ravnica Allegiance #88
 * {3}{B} · Creature — Ogre Warrior · 3 / 3
 *
 * Bloodmist Infiltrator's shape: an [Effects.MayPay] over sacrificing another creature, chosen
 * as the trigger resolves, so "If you do" only adds the two counters and scries 2 when a
 * creature was actually sacrificed (one sacrifice at most — ruling).
 */
val UndercityScavenger = card("Undercity Scavenger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may sacrifice another creature. If you do, " +
        "put two +1/+1 counters on this creature, then scry 2."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self) then
                Effects.Scry(2)
        )
        description = "When this creature enters, you may sacrifice another creature. If you do, " +
            "put two +1/+1 counters on this creature, then scry 2."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Tomasz Jedruszek"
        flavorText = "\"We grow strong by feasting on failure.\"\n—Izoni"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1be7e297-4602-44f8-b919-07015813fd7e.jpg?1783933687"
        ruling("2019-01-25", "You choose whether to sacrifice a creature (and which one to sacrifice) while Undercity Scavenger's ability is resolving. No player may take actions between the time you choose which creature to sacrifice, the time Undercity Scavenger has +1/+1 counters on it, and the time you scry 2.")
        ruling("2019-01-25", "You can't sacrifice multiple creatures to put more +1/+1 counters on Undercity Scavenger or to scry more.")
    }
}
