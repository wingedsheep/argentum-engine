package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deep Goblin Skulltaker — {2}{B}
 * Creature — Goblin Warrior
 * 2/2
 * Menace
 * At the beginning of your end step, if you descended this turn, put a +1/+1 counter on this creature.
 */
val DeepGoblinSkulltaker = card("Deep Goblin Skulltaker") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "Menace\nAt the beginning of your end step, if you descended this turn, put a +1/+1 counter on this creature. (You descended if a permanent card was put into your graveyard from anywhere.)"
    power = 2
    toughness = 2

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouDescendedThisTurn()
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Cristi Balanescu"
        flavorText = "\"Go away! I found it first!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b24bb7c-6b34-48b6-af94-f312ebdbb759.jpg?1782694530"
    }
}
