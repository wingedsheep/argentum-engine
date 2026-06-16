package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Nurturing Pixie
 * {W}
 * Creature — Faerie Rogue
 * 1/1
 *
 * Flying
 * When this creature enters, return up to one target non-Faerie, nonland permanent you control to
 * its owner's hand. If a permanent was returned this way, put a +1/+1 counter on this creature.
 *
 * "Up to one target" is an optional single-target return; the controller may decline. The
 * +1/+1 counter is gated on whether a permanent was actually chosen/returned via
 * [Conditions.TargetMatchesFilter] (`IsPermanent` is a zone-agnostic type-line check, so it
 * still matches after the card moves to hand). When the optional target is declined no target
 * exists, the condition is false, and no counter is placed — mirroring Azure Beastbinder's
 * "up to one target … if it's a creature" resolution-time gate.
 */
val NurturingPixie = card("Nurturing Pixie") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Faerie Rogue"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, return up to one target non-Faerie, nonland permanent you " +
        "control to its owner's hand. If a permanent was returned this way, put a +1/+1 counter " +
        "on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "permanent",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.NonlandPermanent.notSubtype(Subtype.FAERIE).youControl(),
                ),
                optional = true,
            ),
        )
        effect = Effects.ReturnToHand(t).then(
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Permanent),
                effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            ),
        )
        description = "When this creature enters, return up to one target non-Faerie, nonland " +
            "permanent you control to its owner's hand. If a permanent was returned this way, " +
            "put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Iris Compiet"
        flavorText = "\"Count yourself lucky I came upon you. This is not a world of second chances.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fe155f6-888b-41a0-a9a0-be7bea998718.jpg?1712355304"
    }
}
