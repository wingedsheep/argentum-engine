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
 * Tattermunge Duo
 * {2}{R/G}
 * Creature — Goblin Warrior Shaman
 * 2 / 3
 *
 * Whenever you cast a red spell, this creature gets +1/+1 until end of turn.
 * Whenever you cast a green spell, this creature gains forestwalk until end of turn. (It can't be
 * blocked as long as defending player controls a Forest.)
 *
 * - Both triggers are `Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(...))`:
 *   "a red spell" covers every card type, so the filter stays `Any`.
 * - A spell that is both red and green triggers both abilities — correct for the Duo cycle.
 * - Forestwalk is granted with the plain `Effects.GrantKeyword` facade (as Unseen Walker does);
 *   the landwalk evasion check reads runtime-granted keywords.
 */
val TattermungeDuo = card("Tattermunge Duo") {
    manaCost = "{2}{R/G}"
    typeLine = "Creature — Goblin Warrior Shaman"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast a red spell, this creature gets +1/+1 until end of turn.\n" +
        "Whenever you cast a green spell, this creature gains forestwalk until end of turn. (It can't be blocked as long as defending player controls a Forest.)"

    // Whenever you cast a red spell, this creature gets +1/+1 until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.RED))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    // Whenever you cast a green spell, this creature gains forestwalk until end of turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Any.withColor(Color.GREEN))
        effect = Effects.GrantKeyword(Keyword.FORESTWALK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac93faba-8b40-48f4-b4eb-7c8677ed07e2.jpg?1783942719"
    }
}
