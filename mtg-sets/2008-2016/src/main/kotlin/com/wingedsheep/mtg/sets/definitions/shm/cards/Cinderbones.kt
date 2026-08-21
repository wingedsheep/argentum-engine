package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cinderbones
 * {2}{B}
 * Creature — Elemental Skeleton
 * 1 / 1
 *
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 * {1}{B}: Regenerate this creature.
 *
 * - Wither is the plain engine-live keyword; the reminder text is printed on the Shadowmoor card
 *   and is kept verbatim in the oracle text.
 * - "Regenerate this creature" is [RegenerateEffect] on [EffectTarget.Self] — a shield that
 *   replaces the next destruction this turn, not damage prevention, so it does nothing about the
 *   -1/-1 counters wither leaves behind on Cinderbones itself.
 */
val Cinderbones = card("Cinderbones") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Elemental Skeleton"
    power = 1
    toughness = 1
    oracleText = "Wither (This deals damage to creatures in the form of -1/-1 counters.)\n" +
        "{1}{B}: Regenerate this creature."

    keywords(Keyword.WITHER)

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = RegenerateEffect(EffectTarget.Self)
        description = "{1}{B}: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Carl Critchlow"
        flavorText = "Not all coals lie quietly in their beds of cold ash."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22dcf40c-62fb-4205-807e-71655066a61b.jpg?1783942756"
    }
}
