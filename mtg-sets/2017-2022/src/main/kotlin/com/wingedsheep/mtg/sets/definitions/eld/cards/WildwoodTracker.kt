package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wildwood Tracker — Throne of Eldraine #183 (canonical printing)
 * {G} · Creature — Elf Warrior · 1/1
 *
 * Whenever this creature attacks or blocks, if you control another non-Human creature, this
 * creature gets +1/+1 until end of turn.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape).
 * The intervening-if needs `excludeSelf = true`: the Tracker is itself a non-Human (Elf), so
 * without the "another" exclusion it would always satisfy its own condition.
 */
val WildwoodTracker = card("Wildwood Tracker") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Whenever this creature attacks or blocks, if you control another non-Human creature, " +
        "this creature gets +1/+1 until end of turn."
    power = 1
    toughness = 1

    val anotherNonHuman = Conditions.YouControl(
        GameObjectFilter.Creature.notSubtype(Subtype.HUMAN),
        excludeSelf = true
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = anotherNonHuman
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        interveningIf = anotherNonHuman
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Titus Lunter"
        flavorText = "\"This one was wearing shoes made of glass. She'll be no ordinary prey.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3538eba1-475a-4388-8a72-55ab7cd1027e.jpg?1783932600"
    }
}
