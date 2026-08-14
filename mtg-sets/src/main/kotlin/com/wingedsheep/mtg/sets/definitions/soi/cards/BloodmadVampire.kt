package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodmad Vampire (Shadows over Innistrad #146)
 * {2}{R}
 * Creature — Vampire Berserker
 * 4/1
 *
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 * Madness {1}{R}
 *
 * Madness (CR 702.35) on a *permanent* still puts the card on the stack as a normal creature
 * spell — it just costs {1}{R} and is cast while the madness trigger resolves, which is why a
 * creature can be madness-cast during the opponent's turn (it will, of course, be summoning sick).
 */
val BloodmadVampire = card("Bloodmad Vampire") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Berserker"
    power = 4
    toughness = 1
    oracleText = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.\n" +
        "Madness {1}{R} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    madness("{1}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b64e974a-3cf7-49f1-9d5a-c74f920f0169.jpg?1783937758"
    }
}
