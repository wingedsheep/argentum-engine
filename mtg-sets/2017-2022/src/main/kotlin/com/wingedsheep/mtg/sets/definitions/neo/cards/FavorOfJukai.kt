package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Favor of Jukai — Kamigawa: Neon Dynasty #184
 * {3}{G} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets +3/+3 and has reach.
 * Channel — {1}{G}, Discard this card: Target creature gets +3/+3 and gains reach until end of turn.
 *
 * Gift of Wrath's shape: both halves of the bonus are gated by
 * [Conditions.EnchantedPermanentMatches] on the host's current (projected) type, so an uncrewed
 * Vehicle gets nothing.
 */
val FavorOfJukai = card("Favor of Jukai") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets +3/+3 and has reach.\n" +
        "Channel — {1}{G}, Discard this card: Target creature gets +3/+3 and gains reach until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(3, 3, GroupFilter.attachedCreature())
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = GrantKeyword(Keyword.REACH, GroupFilter.attachedCreature())
    }

    // Channel — {1}{G}, Discard this card (from hand): +3/+3 and reach until end of turn.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, creature) then Effects.GrantKeyword(Keyword.REACH, creature)
        description = "Channel — {1}{G}, Discard this card: Target creature gets +3/+3 and gains reach until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Howard Lyon"
        flavorText = "The land chooses its own champions."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1013266a-81f4-4441-8dcf-2764d14afceb.jpg?1783923850"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
