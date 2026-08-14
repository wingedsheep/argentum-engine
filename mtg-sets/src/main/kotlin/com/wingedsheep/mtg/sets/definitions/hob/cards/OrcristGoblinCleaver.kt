package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Orcrist, Goblin-cleaver — The Hobbit #177
 * {3} · Legendary Artifact — Equipment · Mythic
 *
 * Equipped creature gets +2/+2 and has trample.
 * Whenever equipped creature deals combat damage to a player, choose a creature type. Create a
 * Treasure token for each creature you control of that type.
 * Equip {3}
 *
 * Modeling notes:
 *  - The trigger is bound to the *attached* creature ([TriggerBinding.ATTACHED]), the Goldvein Pick
 *    shape, so it follows the Equipment as it moves rather than watching Orcrist itself.
 *  - "Choose a creature type" happens on resolution, not on equip, so it is a
 *    [ChooseCreatureTypeEffect] pipeline step; the count then reads the pick back through
 *    `withSubtypeFromVariable("chosenCreatureType")`. Counting *after* the choice matters — the
 *    board is re-read at that moment, and combat damage has already killed whatever died.
 *  - The count is a battlefield read, so it goes through [DynamicAmount.Count] (projection-aware):
 *    a Rabbit that a lord has turned into a Goblin counts as a Goblin.
 *  - Nothing forces a type with hits on it — naming a type you control none of is legal and simply
 *    makes zero Treasures.
 */
val OrcristGoblinCleaver = card("Orcrist, Goblin-cleaver") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has trample.\n" +
        "Whenever equipped creature deals combat damage to a player, choose a creature type. " +
        "Create a Treasure token for each creature you control of that type.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            DamageType.Combat,
            RecipientFilter.AnyPlayer,
            binding = TriggerBinding.ATTACHED
        )
        effect = Effects.Composite(
            ChooseCreatureTypeEffect,
            Effects.CreateTreasure(
                count = DynamicAmount.Count(
                    Player.You,
                    Zone.BATTLEFIELD,
                    GameObjectFilter.Creature.withSubtypeFromVariable("chosenCreatureType")
                )
            )
        )
        description = "Whenever equipped creature deals combat damage to a player, choose a " +
            "creature type. Create a Treasure token for each creature you control of that type."
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "177"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f54f1c1d-6a22-43e9-a842-0a1ae25b323c.jpg?1784798240"
    }
}
