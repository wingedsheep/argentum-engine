package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Due Diligence — Murders at Karlov Manor #14
 * {2}{W} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, target creature you control other than enchanted creature gets +2/+2 and
 * gains vigilance until end of turn.
 * Enchanted creature gets +2/+2 and has vigilance.
 *
 * Two bodies get the buff, but only one keeps it: the static half rides the attachment for as long
 * as the Aura stays put, while the enters trigger is a one-shot on a *second*, separately-targeted
 * creature that wears off at cleanup.
 *
 * "other than enchanted creature" is the source-relative
 * [GameObjectFilter.notAttachedToBySource] exclusion, evaluated against the Aura's own attachment
 * at the moment targets are chosen and again on resolution — which is exactly what the printed
 * ruling describes: if the Aura somehow moves onto the creature its own trigger is targeting
 * before that trigger resolves, the target is no longer legal, the ability is removed from the
 * stack, and *none* of its effects happen (CR 608.2b).
 *
 * Note that the Aura may legally enchant a creature an opponent controls (the enchant restriction
 * is just "creature"), but the trigger's target is restricted to a creature *you* control.
 */
val DueDiligence = card("Due Diligence") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, target creature you control other than enchanted creature gets " +
        "+2/+2 and gains vigilance until end of turn.\n" +
        "Enchanted creature gets +2/+2 and has vigilance."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val other = target(
            "target creature you control other than enchanted creature",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.youControl().notAttachedToBySource())
            )
        )
        effect = Effects.Composite(
            Effects.ModifyStats(2, 2, other),
            Effects.GrantKeyword(Keyword.VIGILANCE, other)
        )
        description = "When this Aura enters, target creature you control other than enchanted " +
            "creature gets +2/+2 and gains vigilance until end of turn."
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/076d9f76-a247-4727-9e0f-a0289c51059e.jpg?1783912925"

        ruling(
            "2024-02-02",
            "In the unusual case where Due Diligence's second ability triggers and then Due " +
                "Diligence becomes attached to the target of its own triggered ability before " +
                "that ability resolves, when the triggered ability tries to resolve, it won't " +
                "resolve and none of its effects will happen."
        )
    }
}
