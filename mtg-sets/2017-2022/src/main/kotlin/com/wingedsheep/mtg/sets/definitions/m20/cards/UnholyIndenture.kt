package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unholy Indenture — Core Set 2020 #119
 * {2}{B} · Enchantment — Aura
 *
 * Enchant creature
 * When enchanted creature dies, return that card to the battlefield under your control with a
 * +1/+1 counter on it.
 *
 * Minion's Return's attached-bound dies trigger, with the counter folded into the move
 * (`addCounterType`, Razorlash Transmogrant's shape) so the card *enters* with it rather than
 * getting it afterwards. `fromZone = GRAVEYARD` means nothing returns if the card left the
 * graveyard first, and a token (which ceases to exist there) returns nothing.
 */
val UnholyIndenture = card("Unholy Indenture") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When enchanted creature dies, return that card to the battlefield under your control with a " +
        "+1/+1 counter on it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
        )
        description = "When enchanted creature dies, return that card to the battlefield under your control " +
            "with a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Seb McKinnon"
        flavorText = "\"Don't think of it as selling your soul. It's more like you're bequeathing it to me.\"\n" +
            "—Vilis, Broker of Blood"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31e22edb-a815-413d-b62d-f32f8932b6e6.jpg?1783932986"
        ruling(
            "2019-07-12",
            "Once Unholy Indenture returns the creature, it's considered a new object with no relation to the " +
                "object that it was. Auras that were attached to it won't return to the battlefield (including " +
                "Unholy Indenture itself). Equipment that was attached to it will remain unattached. Any " +
                "counters that were on it won't be put on the new creature."
        )
        ruling("2019-07-12", "You control the returned creature, regardless of who controlled it before it died.")
        ruling(
            "2019-07-12",
            "If the returned creature enters with any other counters, it enters with those counters plus an " +
                "additional +1/+1 counter."
        )
        ruling("2019-07-12", "If Unholy Indenture enchants a token, it won't be returned to the battlefield when it dies.")
    }
}
