package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Glamdring — The Lord of the Rings: Tales of Middle-earth #239
 * {2} · Legendary Artifact — Equipment · Mythic
 *
 * Equipped creature has first strike and gets +1/+0 for each instant and sorcery card in your
 * graveyard.
 * Whenever equipped creature deals combat damage to a player, you may cast an instant or sorcery
 * spell from your hand with mana value less than or equal to that damage without paying its mana
 * cost.
 * Equip {3}
 *
 * Composed entirely from existing primitives:
 *   1. [GrantKeyword] first strike + [GrantDynamicStatsEffect] +X/+0 on [Filters.EquippedCreature],
 *      where X is a graveyard-count [DynamicAmount] over instant/sorcery cards in your graveyard.
 *   2. A [Triggers.DealsCombatDamageToPlayer]-shaped trigger bound to the equipped creature
 *      ([TriggerBinding.ATTACHED]). "That damage" is read from the triggering damage event via
 *      [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT] and stored as the free-cast mana-value cap, then
 *      the same Press-the-Enemy free-cast pipeline (gather hand instants/sorceries → keep MV ≤ cap
 *      → optional [Effects.CastFromCollectionWithoutPayingCost]) offers the optional free cast.
 */
val Glamdring = card("Glamdring") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature has first strike and gets +1/+0 for each instant and sorcery " +
        "card in your graveyard.\n" +
        "Whenever equipped creature deals combat damage to a player, you may cast an instant or " +
        "sorcery spell from your hand with mana value less than or equal to that damage without " +
        "paying its mana cost.\n" +
        "Equip {3}"

    // Equipped creature has first strike...
    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature)
    }

    // ...and gets +1/+0 for each instant and sorcery card in your graveyard.
    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = Filters.EquippedCreature,
            powerBonus = DynamicAmounts.zone(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter.InstantOrSorcery
            ).count(),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    // Whenever equipped creature deals combat damage to a player, you may cast an instant or
    // sorcery spell from your hand with MV ≤ that damage without paying its mana cost.
    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            binding = TriggerBinding.ATTACHED
        )
        effect = Effects.Composite(
            // Capture "that damage" — the combat damage just dealt to the player.
            Effects.StoreNumber(
                "combatDamage",
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
            ),
            // Gather instant/sorcery cards from your hand with MV ≤ that damage.
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.You,
                    filter = GameObjectFilter.InstantOrSorcery
                ),
                storeAs = "handSpells"
            ),
            FilterCollectionEffect(
                from = "handSpells",
                filter = CollectionFilter.ManaValueAtMost(DynamicAmount.VariableReference("combatDamage")),
                storeMatching = "castable"
            ),
            // You may cast one of them without paying its mana cost.
            ConditionalOnCollectionEffect(
                collection = "castable",
                ifNotEmpty = MayEffect(Effects.CastFromCollectionWithoutPayingCost("castable"))
            )
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "239"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8296ebf7-45ed-4b38-acf2-b48d9fb3e706.jpg?1686970159"
    }
}
