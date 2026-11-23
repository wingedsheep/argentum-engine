package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warmind Infantry
 * {2}{R}
 * Creature — Elemental Soldier
 * 2/3
 * Battalion — Whenever this creature and at least two other creatures attack, this creature gets
 * +2/+0 until end of turn.
 */
val WarmindInfantry = card("Warmind Infantry") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Soldier"
    power = 2
    toughness = 3
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Greg Staples"
        flavorText = "Before Aurelia denounced Niv-Mizzet, the two guilds had collaborated on the Warmind Initiative in pursuit of the ultimate soldier."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8a5f801-9e55-4e14-85b0-5719521cd9d6.jpg?1783940120"
    }
}
