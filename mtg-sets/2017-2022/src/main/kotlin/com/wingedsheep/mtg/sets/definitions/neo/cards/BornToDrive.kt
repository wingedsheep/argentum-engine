package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Born to Drive — Kamigawa: Neon Dynasty #6
 * {2}{W} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets +1/+1 for each creature and/or Vehicle
 * you control.
 * Channel — {2}{W}, Discard this card: Create two 1/1 colorless Pilot creature tokens with
 * "This token crews Vehicles as though its power were 2 greater."
 *
 * The bonus is Nature's Embrace's shape: a [GrantDynamicStats] over the enchanted permanent, gated
 * by [Conditions.EnchantedPermanentMatches] on the host's *current* (projected) type, so an
 * uncrewed Vehicle gets nothing and a crewed one gets the bonus until end of turn. The count is
 * one battlefield aggregate over [GameObjectFilter.CreatureOrVehicle] — each permanent is counted
 * once, so a crewed Vehicle (a creature *and* a Vehicle) adds 1, not 2. The enchanted permanent
 * counts itself when you control it (no "other"), and "you" is the Aura's controller.
 * The channel half is the NEO channel shape (activated from hand, discard as cost); the Pilot is
 * crew-only, like Hotshot Mechanic — NEO predates saddle.
 */
val BornToDrive = card("Born to Drive") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets +1/+1 for each creature and/or " +
        "Vehicle you control.\n" +
        "Channel — {2}{W}, Discard this card: Create two 1/1 colorless Pilot creature tokens with " +
        "\"This token crews Vehicles as though its power were 2 greater.\""

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    val creaturesAndVehicles = DynamicAmounts.battlefield(Player.You, GameObjectFilter.CreatureOrVehicle).count()

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = creaturesAndVehicles,
            toughnessBonus = creaturesAndVehicles
        )
    }

    // Channel — {2}{W}, Discard this card (from hand): two crew-boosting Pilot tokens.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Pilot"),
            imageUri = "https://cards.scryfall.io/normal/front/b/e/be84f259-2809-48c9-9c70-861437f08c23.jpg?1783923717",
            staticAbilities = listOf(CrewSaddleContribution(modifier = 2, costs = setOf(CrewSaddleCost.CREW)))
        )
        description = "Channel — {2}{W}, Discard this card: Create two 1/1 colorless Pilot creature " +
            "tokens with \"This token crews Vehicles as though its power were 2 greater.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffdfad1f-d316-4023-9c79-8c24f1cd31d0.jpg?1783923925"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}
