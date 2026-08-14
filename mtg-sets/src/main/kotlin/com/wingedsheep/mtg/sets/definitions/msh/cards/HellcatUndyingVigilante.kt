package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellcat, Undying Vigilante — Marvel Super Heroes #170
 * {G}{G} · Legendary Creature — Human Hero · Uncommon
 * 2/2
 *
 * Haste
 * When Hellcat dies, return her to the battlefield under her owner's control with a +1/+1
 * counter on her. She loses all abilities and gains haste.
 *
 * The dies trigger is Retched Wretch's shape: [Triggers.Dies] with [EffectTarget.Self], because
 * the graveyard→battlefield [Effects.Move] keeps the entity id, so every later step in the
 * composite still addresses the returned permanent (CR 611.2b — the grants have no duration and
 * modify the object created by this resolution, ending if it leaves again).
 *
 * Order matters. The ability strip is composed **before** the haste grant so the grant carries
 * the later timestamp and survives it (CR 613.7 within layer 6): stripping first and granting
 * second is exactly what the printed "loses all abilities and gains haste" asks for. Because the
 * strip is permanent, the returned Hellcat no longer has this dies trigger — she comes back once
 * per death, not in a loop.
 */
val HellcatUndyingVigilante = card("Hellcat, Undying Vigilante") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Hero"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "When Hellcat dies, return her to the battlefield under her owner's control with a +1/+1 " +
        "counter on her. She loses all abilities and gains haste."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Composite(
            // Return her to the battlefield under her owner's control *with* a +1/+1 counter on
            // her — `addCounterType`, not a following AddCounters, because "with a counter on it"
            // is an as-enters replacement (CR 614.1c): counter doublers and "enters with an
            // additional counter" effects have to see her enter carrying it.
            Effects.Move(
                EffectTarget.Self,
                Zone.BATTLEFIELD,
                addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
            ),
            // She loses all abilities ...
            Effects.RemoveAllAbilities(EffectTarget.Self, Duration.Permanent),
            // ... and gains haste (granted after the strip, so it has the later timestamp).
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.Permanent)
        )
        description = "When Hellcat dies, return her to the battlefield under her owner's control " +
            "with a +1/+1 counter on her. She loses all abilities and gains haste."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Solan"
        flavorText = "\"I've been to hell and back. You punks barely even register.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7922c5f-d6ee-4b62-a537-3be5aa280e10.jpg?1783902917"
    }
}
