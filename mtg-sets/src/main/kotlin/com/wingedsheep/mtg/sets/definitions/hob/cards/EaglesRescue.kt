package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Eagle's Rescue
 * {2}{W/U}{W/U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2 and has flying.
 * {2}{W/U}{W/U}: Return this card from your graveyard to the battlefield attached to target
 * creature you control with power 1 or less. Activate only as a sorcery.
 *
 * Modeling notes:
 *  - The recursion ability is activated from the graveyard, so it needs
 *    `activateFromZone = Zone.GRAVEYARD` (the Haunted Dead / Morbius idiom); without it the
 *    ability is only ever enumerated on the battlefield and the card is a dead draw once it
 *    hits the yard.
 *  - "Power 1 or less" is measured when the target is chosen *and* re-checked on resolution
 *    (CR 608.2b), so pumping the creature in response fizzles the ability — the filter lives
 *    on the [TargetCreature] requirement rather than in the effect body.
 *  - [Effects.ReturnSelfToBattlefieldAttached] defaults to the triggering entity (Dragon
 *    Wings); here it takes the ability's chosen target instead.
 *  - "Activate only as a sorcery" is [TimingRule.SorcerySpeed], not a separate restriction.
 */
val EaglesRescue = card("Eagle's Rescue") {
    manaCost = "{2}{W/U}{W/U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 and has flying.\n" +
        "{2}{W/U}{W/U}: Return this card from your graveyard to the battlefield attached to " +
        "target creature you control with power 1 or less. Activate only as a sorcery."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EnchantedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W/U}{W/U}")
        target = TargetCreature(filter = TargetFilter.CreatureYouControl.powerAtMost(1))
        effect = Effects.ReturnSelfToBattlefieldAttached(EffectTarget.ContextTarget(0))
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        description = "Return this card from your graveyard to the battlefield attached to " +
            "target creature you control with power 1 or less. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Ramza Psyru"
        flavorText = "\"Don't pinch! You need not be frightened like a rabbit. What is finer than flying?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12c8f2cc-ac9d-4cf6-9025-efe366b4e07f.jpg?1785236726"
    }
}
