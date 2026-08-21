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
 * Safehold Duo
 * {3}{G/W}
 * Creature — Elf Warrior Shaman
 * 2 / 4
 *
 * Whenever you cast a green spell, this creature gets +1/+1 until end of turn.
 * Whenever you cast a white spell, this creature gains vigilance until end of turn.
 *
 * - Both triggers are `Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(...))`:
 *   "a green spell" is not limited to creature spells, so the filter stays `Any`.
 * - A spell that is both green and white triggers both abilities — correct for the Duo cycle.
 * - `EffectTarget.Self` with the facades' default `Duration.EndOfTurn` gives the printed wording.
 */
val SafeholdDuo = card("Safehold Duo") {
    manaCost = "{3}{G/W}"
    typeLine = "Creature — Elf Warrior Shaman"
    power = 2
    toughness = 4
    oracleText = "Whenever you cast a green spell, this creature gets +1/+1 until end of turn.\n" +
        "Whenever you cast a white spell, this creature gains vigilance until end of turn."

    // Whenever you cast a green spell, this creature gets +1/+1 until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.GREEN))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    // Whenever you cast a white spell, this creature gains vigilance until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.WHITE))
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "238"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32b70339-9918-4f7e-9cd0-d4ce36b65997.jpg?1783942715"
    }
}
