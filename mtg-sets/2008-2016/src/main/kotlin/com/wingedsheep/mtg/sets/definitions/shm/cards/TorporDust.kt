package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Torpor Dust
 * {2}{U/B}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets -3/-0.
 *
 * - "Enchant creature" is the `auraTarget`, not a targeting requirement on a spell effect: the Aura
 *   picks its target on cast and attaches on resolution.
 * - The penalty is a flat -3/-0 continuous effect over [GroupFilter.attachedCreature], so it tracks
 *   the creature the Dust is currently attached to (including after a control change or a move).
 * - Flash plus a power-only penalty is the printed combat trick: cast it after blockers, and the
 *   creature survives (toughness untouched) but deals 3 less damage.
 */
val TorporDust = card("Torpor Dust") {
    manaCost = "{2}{U/B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets -3/-0."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(-3, 0, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Jesper Ejsing"
        flavorText = "\"Some folk these days are too restless to dream the dreams we need. We need to teach them to stop and catch their breath.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8840551f-d43a-487f-a960-9b220dec5df4.jpg?1783942728"
    }
}
