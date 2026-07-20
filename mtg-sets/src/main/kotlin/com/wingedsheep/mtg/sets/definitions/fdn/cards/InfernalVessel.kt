package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infernal Vessel
 * {2}{B}
 * Creature — Human Cleric
 * 2/1
 *
 * When this creature dies, if it wasn't a Demon, return it to the battlefield under its owner's
 * control with two +1/+1 counters on it. It's a Demon in addition to its other types.
 *
 * The intervening-if is the loop guard, and it has to read *last-known* information: by the time the
 * trigger is considered, the Vessel is already a card in the graveyard with its printed
 * `Human Cleric` type line, so asking the live entity would answer "not a Demon" forever.
 * [Conditions.TriggeringEntityHadSubtype] reads the projected subtypes captured when the permanent
 * left the battlefield (CR 603.10), which is exactly where the granted Demon type shows up.
 *
 * The three effects run in order on the same entity id — the graveyard → battlefield return keeps
 * it, so `EffectTarget.Self` in the counter and type effects lands on the returned permanent.
 * `fromZone = GRAVEYARD` makes the return a no-op if something else already moved the card out.
 * [Duration.Permanent] on the added type lasts until the permanent next leaves the battlefield,
 * so it is still in force at the moment the snapshot for the *second* death is taken.
 */
val InfernalVessel = card("Infernal Vessel") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, if it wasn't a Demon, return it to the battlefield under " +
        "its owner's control with two +1/+1 counters on it. It's a Demon in addition to its other types."

    triggeredAbility {
        trigger = Triggers.Dies
        triggerCondition = Conditions.Not(Conditions.TriggeringEntityHadSubtype(Subtype.DEMON.value))
        effect = Effects.Composite(
            Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.BATTLEFIELD,
                fromZone = Zone.GRAVEYARD
            ),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            Effects.AddCreatureType(Subtype.DEMON.value, EffectTarget.Self, Duration.Permanent)
        )
        description = "When this creature dies, if it wasn't a Demon, return it to the battlefield " +
            "under its owner's control with two +1/+1 counters on it. It's a Demon in addition to " +
            "its other types."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Franz Vohwinkel"
        flavorText = "\"Rise, my liege! Rise, and blanket the world in eternal night!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/877b6330-2d0b-4f2f-a848-f10b06fb4ef5.jpg?1783909112"
    }
}
