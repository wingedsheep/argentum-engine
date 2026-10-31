package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blood for Bones — Core Set 2020 #89
 * {3}{B} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Return a creature card from your graveyard to the battlefield, then return another creature card
 * from your graveyard to your hand.
 *
 * Neither card is targeted: both are chosen on resolution, so the creature sacrificed as the cost
 * is a legal pick. The graveyard is re-gathered after the first move, so "another" can never be
 * the card that just went to the battlefield.
 */
val BloodForBones = card("Blood for Bones") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Return a creature card from your graveyard to the battlefield, then return another creature card from your graveyard to your hand."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.Pipeline {
            val first = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
            val toBattlefield = chooseExactly(
                1,
                from = first,
                prompt = "Return a creature card from your graveyard to the battlefield"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            val second = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
            val toReturn = chooseExactly(
                1,
                from = second,
                prompt = "Return another creature card from your graveyard to your hand"
            )
            toHand(toReturn)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7892b516-0dce-491f-8b42-031d64397e26.jpg?1783932999"
    }
}
