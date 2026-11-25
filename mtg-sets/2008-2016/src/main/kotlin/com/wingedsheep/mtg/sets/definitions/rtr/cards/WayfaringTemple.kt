package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Wayfaring Temple
 * {1}{G}{W}
 * Creature — Elemental
 * * / *
 * Wayfaring Temple's power and toughness are each equal to the number of creatures you control.
 * Whenever this creature deals combat damage to a player, populate. (Create a token that's a copy
 * of a creature token you control.)
 *
 * The `*`/`*` is a characteristic-defining ability, so it lives on [dynamicStats] (as on Seraph of
 * the Masses) and applies in every zone. Populate is the same shape as Horncaller's Chant:
 * [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens you control (chosen, not targeted).
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val WayfaringTemple = card("Wayfaring Temple") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Elemental"
    oracleText = "Wayfaring Temple's power and toughness are each equal to the number of creatures you control.\n" +
        "Whenever this creature deals combat damage to a player, populate. (Create a token that's a copy of " +
        "a creature token you control.)"

    dynamicStats(DynamicAmounts.creaturesYouControl())

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2125e6aa-f916-4dfa-a9fe-82bb546012af.jpg?1783940328"
    }
}
