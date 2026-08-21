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
 * Thistledown Duo
 * {2}{W/U}
 * Creature — Kithkin Soldier Wizard
 * 2 / 2
 *
 * Whenever you cast a white spell, this creature gets +1/+1 until end of turn.
 * Whenever you cast a blue spell, this creature gains flying until end of turn.
 *
 * - Both triggers are `Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(...))`:
 *   "a white spell" is not limited to creature spells, so the filter stays `Any`.
 * - A spell that is both white and blue triggers both abilities — correct for the Duo cycle.
 * - `EffectTarget.Self` with the facades' default `Duration.EndOfTurn` gives the printed wording.
 */
val ThistledownDuo = card("Thistledown Duo") {
    manaCost = "{2}{W/U}"
    typeLine = "Creature — Kithkin Soldier Wizard"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a white spell, this creature gets +1/+1 until end of turn.\n" +
        "Whenever you cast a blue spell, this creature gains flying until end of turn."

    // Whenever you cast a white spell, this creature gets +1/+1 until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.WHITE))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    // Whenever you cast a blue spell, this creature gains flying until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.BLUE))
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Zoltan Boros & Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d57ba0ce-0390-42fd-9473-2436fac53631.jpg?1783942735"
    }
}
