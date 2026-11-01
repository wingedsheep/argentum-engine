package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dovin's Acuity — Ravnica Allegiance #168
 * {1}{W}{U} · Enchantment
 *
 * "During your main phase" narrows the cast event itself (CR 603.2), so it is a
 * [triggerRestriction] checked as the instant is cast, not an intervening "if". The return is a
 * "you may" and resolves before the instant — even if that instant is countered.
 */
val DovinsAcuity = card("Dovin's Acuity") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you gain 2 life and draw a card.\n" +
        "Whenever you cast an instant spell during your main phase, you may return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(2) then Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Instant)
        triggerRestriction = Conditions.IsYourMainPhase
        optional = true
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Bastien L. Deharme"
        flavorText = "Dovin ticked the Planeswalkers off on his fingers: himself, Domri, Kaya, Ral, Vraska . . . and Bolas made an even six."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b191592-7221-422a-8b5a-65f7b1caec1a.jpg?1783933652"
    }
}
