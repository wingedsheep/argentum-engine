package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fae Flight — Murders at Karlov Manor #56
 * {1}{U} · Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, enchanted creature gains hexproof until end of turn.
 * Enchanted creature gets +1/+0 and has flying.
 *
 * The hexproof is a *one-shot* on the enters trigger, not a static — that's the whole trick of
 * the card: flashed in response to removal, the Aura is already attached when the trigger
 * resolves, so [EffectTarget.EnchantedCreature] resolves through the attachment and the
 * creature dodges the spell already on the stack. Modelling it as a fourth static ability would
 * be wrong twice over — it would persist past this turn, and it would come back if the Aura
 * were ever re-attached.
 *
 * Note the trigger targets nothing (CR 702.155a "enchanted creature" is not a target), so
 * shroud/hexproof on the enchanted creature can't stop it.
 *
 * `AuraEntersGrantsHexproofTest` already proves this exact wiring end-to-end: removal on the
 * stack, Aura flashed in, ETB trigger resolves, removal is countered on resolution (CR 608.2b).
 */
val FaeFlight = card("Fae Flight") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, enchanted creature gains hexproof until end of turn.\n" +
        "Enchanted creature gets +1/+0 and has flying."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GrantHexproof(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, enchanted creature gains hexproof until end of turn."
    }

    staticAbility {
        ability = ModifyStats(1, 0)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Durion"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9caa4eb-ed8c-4d05-8029-2a42163938a7.jpg?1783912911"
    }
}
