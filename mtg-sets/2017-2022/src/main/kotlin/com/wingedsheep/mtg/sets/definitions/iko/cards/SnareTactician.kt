package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Snare Tactician
 * {2}{W}
 * Creature — Human Soldier
 * 2/3
 * Whenever you cycle a card, tap target creature an opponent controls.
 *
 * The same `Triggers.you.cycles()` atom Drannith Stinger uses, into [Effects.Tap] on a creature an
 * opponent controls (Captivating Unicorn's shape).
 */
val SnareTactician = card("Snare Tactician") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Whenever you cycle a card, tap target creature an opponent controls."

    triggeredAbility {
        trigger = Triggers.you.cycles()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature)
        description = "Whenever you cycle a card, tap target creature an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Micah Epstein"
        flavorText = "\"Our volleys don't aim to kill. Impair them and the ground will do the rest.\"\n—Alux, Skysail defender"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f640e0c-298d-4a5f-94d0-d839cf06ca86.jpg?1783931084"
        ruling("2020-04-17", "Some cards with cycling have an ability that triggers when you cycle them, and some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2020-04-17", "You can cycle a card even if it has a triggered ability from cycling that won't have a legal target. This is because the cycling ability and the triggered ability are separate. This also means that if either ability is countered (with Disallow, for example), the other ability will still resolve.")
    }
}
