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
 * Emberstrike Duo
 * {1}{B/R}
 * Creature — Elemental Warrior Shaman
 * 1 / 1
 *
 * Whenever you cast a black spell, this creature gets +1/+1 until end of turn.
 * Whenever you cast a red spell, this creature gains first strike until end of turn.
 *
 * - Both triggers are `Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(...))`:
 *   the spell filter matches *any* card type, since "a black spell" is not restricted to creatures.
 * - The colour test is on the spell itself, so a hybrid or multicoloured spell that is (say) both
 *   black and red triggers *both* abilities — that is correct for the Duo cycle.
 * - `EffectTarget.Self` and the default `Duration.EndOfTurn` on both effects give the printed
 *   "this creature ... until end of turn" wording; neither trigger targets.
 */
val EmberstrikeDuo = card("Emberstrike Duo") {
    manaCost = "{1}{B/R}"
    typeLine = "Creature — Elemental Warrior Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever you cast a black spell, this creature gets +1/+1 until end of turn.\n" +
        "Whenever you cast a red spell, this creature gains first strike until end of turn."

    // Whenever you cast a black spell, this creature gets +1/+1 until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.BLACK))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    // Whenever you cast a red spell, this creature gains first strike until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.RED))
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ccd4374-5339-4529-99f3-f7dcc939e874.jpg?1783942727"
    }
}
