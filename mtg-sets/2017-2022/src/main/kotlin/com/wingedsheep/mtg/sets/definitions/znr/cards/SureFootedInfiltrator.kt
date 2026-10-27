package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sure-Footed Infiltrator — Zendikar Rising #83
 * {3}{U} · Creature — Merfolk Rogue · 2/3
 *
 * Tap another untapped Rogue you control: This creature can't be blocked this turn.
 * Whenever this creature deals combat damage to a player, draw a card.
 *
 * The cost is [Costs.TapAnotherPermanent] — "another" excludes the Infiltrator itself, and since the
 * cost has no {T} symbol, neither the Infiltrator nor the tapped Rogue is bound by summoning sickness.
 */
val SureFootedInfiltrator = card("Sure-Footed Infiltrator") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    oracleText = "Tap another untapped Rogue you control: This creature can't be blocked this turn.\n" +
        "Whenever this creature deals combat damage to a player, draw a card."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.TapAnotherPermanent(GameObjectFilter.Permanent.withSubtype("Rogue"))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Tap another untapped Rogue you control: This creature can't be blocked this turn."
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
        description = "Whenever this creature deals combat damage to a player, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Mathias Kollros"
        flavorText = "\"I've yet to discover the ruin that can keep me out.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0505053c-8a9c-43ed-b44a-b52c56ba4f77.jpg"
    }
}
