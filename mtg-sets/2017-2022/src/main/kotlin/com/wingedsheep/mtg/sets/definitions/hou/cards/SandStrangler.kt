package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sand Strangler — Hour of Devastation #107 (canonical printing)
 * {3}{R} · Creature — Beast · 3/3
 *
 * When this creature enters, if you control a Desert or there is a Desert card in your graveyard,
 * you may have this creature deal 3 damage to target creature.
 *
 * The Desert clause is an intervening-if (CR 603.4), checked as the trigger fires and again on
 * resolution, with either half satisfying each check — the same composition as Wretched Camel.
 * The "you may" is the trigger's optional choice on resolution.
 */
val SandStrangler = card("Sand Strangler") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, if you control a Desert or there is a Desert card in your graveyard, " +
        "you may have this creature deal 3 damage to target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Any(
            Conditions.ControlPermanentOfType(Subtype.DESERT),
            Conditions.GraveyardContainsSubtype(Subtype.DESERT),
        )
        optional = true
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Aaron Miller"
        flavorText = "The bombardment of sand disorients, chokes, and ultimately tenderizes its prey."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd7153be-ad6c-47ff-8f45-bc8df17973cb.jpg?1783936024"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn’t matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
        ruling("2017-07-14", "For abilities that trigger only if you control a Desert or there is a Desert card in your graveyard, one condition must be true as the ability triggers and one must be true as the ability resolves. They don’t have to be the same condition, though. For example, you could sacrifice your only Desert after the ability triggers but before it has resolved.")
    }
}
