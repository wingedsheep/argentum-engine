package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Inertia Bubble — Mirrodin #37 (canonical printing, only printing)
 * {1}{U} · Enchantment — Aura
 *
 * Enchant artifact
 * Enchanted artifact doesn't untap during its controller's untap step.
 *
 * The Charmed Sleep shape pointed at an artifact instead of a creature: the bare
 * [GrantKeyword] static with no filter scopes to the attached permanent. Note the Aura only stops
 * the untap step — it doesn't tap the artifact on entry, so against an untapped artifact it does
 * nothing until that artifact taps itself.
 */
val InertiaBubble = card("Inertia Bubble") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\nEnchanted artifact doesn't untap during its controller's untap step."

    auraTarget = Targets.Artifact

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Hugh Jamieson"
        flavorText = "\"I wouldn't want you to hurt yourself.\"\n—Bruenna, Neurok leader"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72b2d227-68b3-40e8-bef7-45ea43e17318.jpg?1783944554"
    }
}
