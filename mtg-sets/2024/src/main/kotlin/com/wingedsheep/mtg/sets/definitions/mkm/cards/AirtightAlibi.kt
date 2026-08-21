package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Airtight Alibi — Murders at Karlov Manor #149
 * {2}{G} · Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, untap enchanted creature. It gains hexproof until end of turn. If it's
 * suspected, it's no longer suspected.
 * Enchanted creature gets +2/+2 and can't become suspected.
 *
 * A flash Aura that answers a suspect — the alibi clears the creature's name and keeps it clear.
 * The two clauses do different jobs and both are needed: the enters trigger takes an *existing*
 * designation off, and the static stops a new one attaching. Neither implies the other.
 *
 * **"If it's suspected, it's no longer suspected"** is [Effects.NoLongerSuspected] with no condition
 * wrapped around it: the effect is already a no-op on an unsuspected creature (CR 701.60c), so the
 * printed "if" is a reminder of that rather than a separate check. Being un-suspected lifts the
 * whole suspect bundle — the designation together with the menace and can't-block that existed only
 * for as long as it was suspected.
 *
 * **"Can't become suspected"** is [AbilityFlag.CANT_BECOME_SUSPECTED], read by the single shared
 * suspect implementation, so it blocks *every* source: a spell, an opponent's triggered ability, or
 * the creature's own "when this attacks, suspect it" clause. It has to gate all three halves of
 * suspect at once — a creature that dodged only the designation but still picked up menace and
 * can't-block would be strictly worse off than an unenchanted one.
 *
 * The hexproof is granted to the creature until end of turn (a one-shot from the trigger), not a
 * static — the card grants it once on arrival, so an Aura that survives to the next turn gives no
 * further protection. The +2/+2 is the only continuous half besides the prohibition.
 */
val AirtightAlibi = card("Airtight Alibi") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, untap enchanted creature. It gains hexproof until end of turn. " +
        "If it's suspected, it's no longer suspected.\n" +
        "Enchanted creature gets +2/+2 and can't become suspected."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.Untap(EffectTarget.EnchantedCreature),
            Effects.GrantHexproof(EffectTarget.EnchantedCreature),
            Effects.NoLongerSuspected(EffectTarget.EnchantedCreature)
        )
        description = "When this Aura enters, untap enchanted creature. It gains hexproof until " +
            "end of turn. If it's suspected, it's no longer suspected."
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.CANT_BECOME_SUSPECTED.name, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Jeremy Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bffbbe21-0a1d-48b9-903e-81c109aa11de.jpg?1783912873"
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until " +
                "it leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
        ruling(
            "2024-02-02",
            "Being suspected isn't a copiable value. If a permanent becomes a copy of a suspected " +
                "creature, it won't be suspected."
        )
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
        ruling(
            "2024-02-02",
            "There's no limit to the number of creatures that can be suspected simultaneously. " +
                "Suspecting a new creature doesn't cause other creatures to stop being suspected."
        )
    }
}
