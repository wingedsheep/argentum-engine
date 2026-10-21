package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Kenriths' Royal Funeral {2}{W}{B}
 * Legendary Enchantment
 *
 * When The Kenriths' Royal Funeral enters, exile up to two target legendary creature cards from
 * your graveyard. You draw X cards and you lose X life, where X is the greatest mana value among
 * cards exiled this way.
 * Legendary spells you cast cost {1} less to cast for each card exiled with The Kenriths' Royal
 * Funeral.
 *
 * The enters trigger gathers its still-legal targets and exiles them with `linkToSource`, so they
 * land in the Funeral's linked-exile pile (CR 607 linked abilities). Only cards that actually moved
 * ("exiled this way") are narrowed to the greatest mana value; X is stored once and read by both
 * the draw and the life loss. No cards exiled means X = 0.
 *
 * The discount counts the linked-exile pile at cast time; a card that leaves exile is unlinked by
 * the zone mover, so it stops counting. It reduces generic mana only.
 */
val TheKenrithsRoyalFuneral = card("The Kenriths' Royal Funeral") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Enchantment"
    oracleText = "When The Kenriths' Royal Funeral enters, exile up to two target legendary creature " +
        "cards from your graveyard. You draw X cards and you lose X life, where X is the greatest mana " +
        "value among cards exiled this way.\n" +
        "Legendary spells you cast cost {1} less to cast for each card exiled with The Kenriths' " +
        "Royal Funeral."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Creature.legendary().ownedByYou(), zone = Zone.GRAVEYARD),
            count = 2,
            optional = true
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE), linkToSource = true)
            val greatest = filter(exiled, CollectionFilter.GreatestManaValue)
            val x = storeNumber(DynamicAmounts.manaValueOf(greatest))
            run(Effects.DrawCards(x.amount))
            run(Effects.LoseLife(x.amount, EffectTarget.Controller))
        }
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.legendary()),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.linkedExileCardCount())
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4569fb73-3555-4eb8-933a-130770dd56d1.jpg?1783916514"
    }
}
