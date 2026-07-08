package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Quintorius Kand
 * {3}{R}{W}
 * Legendary Planeswalker — Quintorius
 * Starting loyalty 4
 *
 * Whenever you cast a spell from exile, Quintorius Kand deals 2 damage to each opponent and you gain 2 life.
 * +1: Create a 3/2 red and white Spirit creature token.
 * −3: Discover 4.
 * −6: Exile any number of target cards from your graveyard. Add {R} for each card exiled this way.
 *     You may play those cards this turn.
 *
 * The cast-from-exile trigger pairs with −3's Discover (and −6's play-from-exile): a card cast for
 * free by discovering, or played from exile via −6, is cast from exile and so triggers the drain.
 */
val QuintoriusKand = card("Quintorius Kand") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Quintorius"
    startingLoyalty = 4
    oracleText = "Whenever you cast a spell from exile, Quintorius Kand deals 2 damage to each opponent and you gain 2 life.\n" +
        "+1: Create a 3/2 red and white Spirit creature token.\n" +
        "−3: Discover 4.\n" +
        "−6: Exile any number of target cards from your graveyard. Add {R} for each card exiled this way. You may play those cards this turn."

    // Whenever you cast a spell from exile, deal 2 damage to each opponent and gain 2 life.
    triggeredAbility {
        trigger = Triggers.youCastSpell(requires = setOf(SpellCastPredicate.CastFromZone(Zone.EXILE)))
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
            .then(Effects.GainLife(2))
    }

    // +1: Create a 3/2 red and white Spirit creature token.
    loyaltyAbility(+1) {
        effect = CreateTokenEffect(
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Spirit"),
            imageUri = "https://cards.scryfall.io/normal/front/0/7/072cd10b-98d3-452e-bf8d-13a75cc3d72e.jpg?1782731573"
        )
    }

    // −3: Discover 4.
    loyaltyAbility(-3) {
        effect = Effects.Discover(4)
    }

    // −6: Exile any number of target cards from your graveyard. Add {R} for each card exiled this
    // way. You may play those cards this turn.
    loyaltyAbility(-6) {
        target(
            "any number of target cards from your graveyard",
            TargetObject(
                unlimited = true,
                filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "kandGathered"),
                MoveCollectionEffect(
                    from = "kandGathered",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    storeMovedAs = "kandExiled"
                ),
                AddManaEffect(Color.RED, DynamicAmount.DistinctEntitiesInCollections(listOf("kandExiled"))),
                Effects.GrantMayPlayFromExile("kandExiled", MayPlayExpiry.EndOfTurn)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "238"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4382fa49-9e34-45b3-8495-4916dcd995ec.jpg?1782694421"
    }
}
