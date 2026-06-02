package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Metathran Aerostat
 * {2}{U}{U}
 * Creature — Metathran
 * 2/2
 * Flying
 * {X}{U}: You may put a creature card with mana value X from your hand onto the battlefield.
 * If you do, return this creature to its owner's hand.
 *
 * The {X}{U} activation stamps X onto the ability's context, so the candidate filter uses
 * [CardPredicate.ManaValueEqualsX] (cf. [Void]). Gather creatures of that mana value from hand →
 * optionally select one (the "may") → put it onto the battlefield. If a creature was actually put,
 * [ConditionalOnCollectionEffect] returns the Aerostat itself to its owner's hand.
 */
val MetathranAerostat = card("Metathran Aerostat") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Metathran"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "{X}{U}: You may put a creature card with mana value X from your hand onto the battlefield. " +
        "If you do, return this creature to its owner's hand."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{X}{U}")
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.HAND,
                        player = Player.You,
                        filter = GameObjectFilter.Creature.copy(
                            cardPredicates = GameObjectFilter.Creature.cardPredicates +
                                CardPredicate.ManaValueEqualsX,
                        ),
                    ),
                    storeAs = "aerostatCandidates",
                ),
                SelectFromCollectionEffect(
                    from = "aerostatCandidates",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "aerostatPutting",
                    prompt = "You may put a creature card with that mana value onto the battlefield",
                ),
                MoveCollectionEffect(
                    from = "aerostatPutting",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                ),
                ConditionalOnCollectionEffect(
                    collection = "aerostatPutting",
                    ifNotEmpty = Effects.ReturnToHand(EffectTarget.Self),
                ),
            ),
        )
        description = "{X}{U}: You may put a creature card with mana value X from your hand onto " +
            "the battlefield. If you do, return this creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59f34850-fb6f-4ac5-8309-4d53d770e28c.jpg?1562913282"
    }
}
