package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gravelgill Duo
 * {2}{U/B}
 * Creature — Merfolk Rogue Warrior
 * 2 / 1
 *
 * Whenever you cast a blue spell, this creature gets +1/+1 until end of turn.
 * Whenever you cast a black spell, this creature gains fear until end of turn. (It can't be blocked
 * except by artifact creatures and/or black creatures.)
 *
 * - Both triggers are `Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(...))`:
 *   "a blue spell" covers every card type, so the filter stays `Any`.
 * - A spell that is both blue and black triggers both abilities — correct for the Duo cycle.
 * - Fear is granted with the plain `Effects.GrantKeyword` facade (as Shriek of Dread does); the
 *   evasion check reads runtime-granted keywords, so no separate evasion effect is needed.
 */
val GravelgillDuo = card("Gravelgill Duo") {
    manaCost = "{2}{U/B}"
    typeLine = "Creature — Merfolk Rogue Warrior"
    power = 2
    toughness = 1
    oracleText = "Whenever you cast a blue spell, this creature gets +1/+1 until end of turn.\n" +
        "Whenever you cast a black spell, this creature gains fear until end of turn. (It can't be blocked except by artifact creatures and/or black creatures.)"

    // Whenever you cast a blue spell, this creature gets +1/+1 until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.BLUE))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    // Whenever you cast a black spell, this creature gains fear until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.BLACK))
        effect = Effects.GrantKeyword(Keyword.FEAR, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Brandon Kitkouski"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2fd4959-bfff-46dc-b568-b6d69ef8eac9.jpg?1783942733"
    }
}
