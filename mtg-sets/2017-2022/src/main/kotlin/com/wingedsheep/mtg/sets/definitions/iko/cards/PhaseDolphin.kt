package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Phase Dolphin — Ikoria: Lair of Behemoths #62
 * {2}{U} · Creature — Elemental Whale · 1/4
 *
 * Whenever this creature attacks, another target attacking creature can't be blocked this turn.
 *
 * "Another target attacking creature" excludes the Dolphin itself via [TargetFilter.other].
 */
val PhaseDolphin = card("Phase Dolphin") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Whale"
    power = 1
    toughness = 4
    oracleText = "Whenever this creature attacks, another target attacking creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.AttackingCreature.other())
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Lie Setiawan"
        flavorText = "\"You've never had to trust your breath to an inhuman creature who could crush you " +
            "at its whim, and I truly feel sorry for you.\"\n—Eagan, seabonder"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88fb4042-e9c1-4b0f-b1f6-4180b0d79663.jpg"
    }
}
