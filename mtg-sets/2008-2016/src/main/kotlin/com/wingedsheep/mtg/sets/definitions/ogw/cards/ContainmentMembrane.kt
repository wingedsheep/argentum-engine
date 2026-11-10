package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Containment Membrane
 * {2}{U}
 * Enchantment — Aura
 * Surge {U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Enchant creature
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Surge is a pure alternative cost here; the surge cast still picks the Aura's target (the
 * keyword-alternative-cast path folds in `castAuraTarget`). "Doesn't untap" reuses the untap
 * step's [AbilityFlag.DOESNT_UNTAP] handling, like Sleep Magic and Flood the Engine.
 */
val ContainmentMembrane = card("Containment Membrane") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Surge {U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Enchant creature\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    keywordAbility(KeywordAbility.surge("{U}"))

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49dd3963-a4d7-4992-b6f8-753996390bbf.jpg?1783937919"
        ruling("2016-01-22", "Containment Membrane can target and be attached to an untapped creature. It just won't have any effect until that creature becomes tapped.")
        ruling("2016-01-22", "The enchanted creature can still be untapped by other spells and abilities.")
    }
}
