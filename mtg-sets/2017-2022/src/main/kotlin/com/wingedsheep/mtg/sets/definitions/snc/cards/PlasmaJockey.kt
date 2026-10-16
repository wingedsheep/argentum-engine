package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Plasma Jockey
 * {3}{R}
 * Creature — Lizard Warrior
 * 3/1
 * Whenever this creature attacks, target creature an opponent controls can't block this turn.
 * Blitz {2}{R} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val PlasmaJockey = card("Plasma Jockey") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    oracleText = "Whenever this creature attacks, target creature an opponent controls can't block this turn.\n" +
        "Blitz {2}{R} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantBlock(victim)
        description = "Whenever this creature attacks, target creature an opponent controls can't block this turn."
    }

    keywordAbility(KeywordAbility.blitz("{2}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/741ed811-cbcd-4178-a874-dc79d2a5d6f1.jpg?1783923117"
    }
}
