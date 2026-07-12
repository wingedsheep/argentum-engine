package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Militia Rallier
 * {2}{W}
 * Creature — Human Soldier
 * 3/3
 *
 * This creature can't attack alone.
 * Whenever this creature attacks, untap target creature.
 */
val MilitiaRallier = card("Militia Rallier") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "This creature can't attack alone.\n" +
        "Whenever this creature attacks, untap target creature."

    // "Can't attack alone" — requires any other creature to also attack.
    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val creature = target("creature", Targets.Creature)
        effect = Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Eelis Kyttanen"
        flavorText = "\"We can wait like helpless children for a hero, or we can gather our courage " +
            "and be the heroes. Who's with me?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51b3ff40-3185-4369-985e-17613bb6ad22.jpg?1782703178"
    }
}
