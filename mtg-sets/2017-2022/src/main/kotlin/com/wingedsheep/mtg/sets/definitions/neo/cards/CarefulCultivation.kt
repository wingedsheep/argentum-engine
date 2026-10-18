package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Careful Cultivation — Kamigawa: Neon Dynasty #178
 * {2}{G} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets +1/+3 and has reach and "{T}: Add {G}{G}."
 * Channel — {1}{G}, Discard this card: Create a 1/1 green Human Monk creature token with "{T}: Add {G}."
 *
 * Gift of Wrath's shape plus Nature's Embrace's conditional mana-ability grant: every part of the
 * bonus is gated by [Conditions.EnchantedPermanentMatches] on the host's current (projected) type,
 * so an uncrewed Vehicle gets nothing.
 */
val CarefulCultivation = card("Careful Cultivation") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets +1/+3 and has reach and \"{T}: Add {G}{G}.\"\n" +
        "Channel — {1}{G}, Discard this card: Create a 1/1 green Human Monk creature token with \"{T}: Add {G}.\""

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(1, 3, GroupFilter.attachedCreature())
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = GrantKeyword(Keyword.REACH, GroupFilter.attachedCreature())
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN, 2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            )
        )
    }

    // Channel — {1}{G}, Discard this card (from hand): a mana-producing Human Monk token.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Human", "Monk"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.Tap,
                    effect = Effects.AddMana(Color.GREEN),
                    isManaAbility = true,
                    timing = TimingRule.ManaAbility,
                    descriptionOverride = "{T}: Add {G}."
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/8/6/86dd086e-c757-4a6a-8838-51860f2095f8.jpg?1783923713"
        )
        description = "Channel — {1}{G}, Discard this card: Create a 1/1 green Human Monk creature token " +
            "with \"{T}: Add {G}.\""
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "A. M. Sartor"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e2ebad5-0a56-4166-80af-7ebd20ae565a.jpg?1783923852"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}
