package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evershrike's Gift
 * {W}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+0 and has flying.
 * {1}{W}, Blight 2: Return this card from your graveyard to your hand. Activate only as a sorcery.
 */
val EvershrikesGift = card("Evershrike's Gift") {
    manaCost = "{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+0 and has flying.\n" +
        "{1}{W}, Blight 2: Return this card from your graveyard to your hand. Activate only as a sorcery. " +
        "(To blight 2, put two -1/-1 counters on a creature you control.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Blight(2))
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Drew Tucker"
        flavorText = "Their first encounter was luck. Their bond was fate."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/231e46b6-b91a-4582-8894-e7de1c50213f.jpg?1767956938"
    }
}
