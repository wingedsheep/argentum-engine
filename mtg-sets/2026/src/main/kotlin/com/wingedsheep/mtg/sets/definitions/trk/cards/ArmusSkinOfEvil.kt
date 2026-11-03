package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Armus, Skin of Evil
 * {5}{B}{B}
 * Legendary Creature — Ooze Horror
 * 6/6
 *
 * Menace
 * At the beginning of your end step, target opponent discards two cards.
 * {2}{B}, Discard this card: Target creature gets -4/-4 until end of turn.
 *
 * The hand ability is a channel-shaped activation: activated from hand with a discard-self cost.
 */
val ArmusSkinOfEvil = card("Armus, Skin of Evil") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Ooze Horror"
    power = 6
    toughness = 6
    oracleText = "Menace\n" +
        "At the beginning of your end step, target opponent discards two cards.\n" +
        "{2}{B}, Discard this card: Target creature gets -4/-4 until end of turn."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(count = 2, target = opponent)
        description = "At the beginning of your end step, target opponent discards two cards."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-4, -4, t)
        description = "{2}{B}, Discard this card: Target creature gets -4/-4 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Camille Alquier"
        flavorText = "The Vagrans cast off their evil, and it coalesced into a singular will."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3ba1e0d-5e3d-4fa7-9b58-5b90944b43c5.jpg?1793013559"
    }
}
