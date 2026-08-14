package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dancing from Dark to Dawn
 * {3}{G}{G}
 * Enchantment
 * Whenever you cast a creature spell, put X +1/+1 counters on target creature you control, where X
 * is that spell's mana value.
 * Landfall — Whenever a land you control enters, create a 2/2 green Bear creature token.
 *
 *  - **"that spell's mana value"** is [DynamicAmounts.triggeringManaValue] — the triggering entity of
 *    [Triggers.YouCastCreature] is the spell on the stack, so the amount is read at resolution off the
 *    spell itself. It reads the spell's mana value on the stack, which for an X spell includes the
 *    chosen X (CR 202.3b), and cost reductions never change it.
 *  - The counters go on **target creature you control**, a different object from the spell that
 *    triggered the ability. The trigger goes on the stack above the creature spell and resolves first,
 *    so the creature being cast is not yet on the battlefield and can't be chosen.
 *  - **Landfall** is [Triggers.LandYouControlEnters] — any land, not just one played for the turn, and
 *    it fires for lands put onto the battlefield by other effects too.
 */
val DancingFromDarkToDawn = card("Dancing from Dark to Dawn") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a creature spell, put X +1/+1 counters on target creature you " +
        "control, where X is that spell's mana value.\n" +
        "Landfall — Whenever a land you control enters, create a 2/2 green Bear creature token."

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        val t = target(
            "target creature you control to get +1/+1 counters",
            TargetCreature(filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.triggeringManaValue(),
            t
        )
        description = "Whenever you cast a creature spell, put X +1/+1 counters on target creature " +
            "you control, where X is that spell's mana value."
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Bear"),
            controller = EffectTarget.Controller,
            imageUri = "https://cards.scryfall.io/normal/front/3/1/31661af9-a40a-418c-82e3-b74aa14cc7c4.jpg?1785497718",
        )
        description = "Landfall — Whenever a land you control enters, create a 2/2 green Bear " +
            "creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "123"
        artist = "Leesha Hannigan"
        flavorText = "\"There must have been a regular bears' meeting outside here last night.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/550cd0b6-ca61-4db7-9d20-0b68c48066f9.jpg?1785236704"
    }
}
