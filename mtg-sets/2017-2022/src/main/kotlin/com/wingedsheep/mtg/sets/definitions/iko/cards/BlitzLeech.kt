package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blitz Leech
 * {5}{B}
 * Creature — Leech
 * 5/2
 * Flash
 * When this creature enters, target creature an opponent controls gets -2/-2 until end of turn.
 * Remove all counters from that creature.
 *
 * Both halves happen during resolution, so the counters are gone before state-based actions look
 * at the creature (2020-04-17 ruling).
 */
val BlitzLeech = card("Blitz Leech") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Leech"
    power = 5
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, target creature an opponent controls gets -2/-2 until end of turn. " +
        "Remove all counters from that creature."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-2, -2, creature) then Effects.RemoveAllCounters(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Nicholas Gregory"
        flavorText = "\"Fools wear armor against it as if the worst it could take is your blood.\"\n—Gavi, nest warden"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d9b5bde-a851-480b-b45e-d384fd1c11bb.jpg?1783931066"
        ruling("2020-04-17", "Blitz Leech's ability removes all counters from the target creature before state-based actions are checked to see if that creature should die. For example, a 2/2 creature with a flying counter on it will lose that counter while it's a 0/0 creature before it dies.")
    }
}
