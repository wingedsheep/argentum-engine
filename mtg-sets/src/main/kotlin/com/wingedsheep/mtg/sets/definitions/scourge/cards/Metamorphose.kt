package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Metamorphose
 * {1}{U}
 * Instant
 * Put target permanent an opponent controls on top of its owner's library.
 * That opponent may put an artifact, creature, enchantment, or land card
 * from their hand onto the battlefield.
 */
val Metamorphose = card("Metamorphose") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Put target permanent an opponent controls on top of its owner's library. That opponent may put an artifact, creature, enchantment, or land card from their hand onto the battlefield."

    spell {
        val permanent = target("permanent an opponent controls", Targets.PermanentOpponentControls)
        effect = CompositeEffect(
            listOf(
                // Put targeted permanent on top of its owner's library
                Effects.PutOnTopOfLibrary(permanent),
                // That opponent may put a permanent card from their hand onto the battlefield
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        Zone.HAND, Player.Opponent,
                        GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.Or(
                                    listOf(
                                        CardPredicate.IsArtifact,
                                        CardPredicate.IsCreature,
                                        CardPredicate.IsEnchantment,
                                        CardPredicate.IsLand
                                    )
                                )
                            )
                        )
                    ),
                    storeAs = "put_candidates"
                ),
                SelectFromCollectionEffect(
                    from = "put_candidates",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Opponent,
                    storeSelected = "putting",
                    prompt = "You may put an artifact, creature, enchantment, or land card from your hand onto the battlefield"
                ),
                MoveCollectionEffect(
                    from = "putting",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Ron Spencer"
        flavorText = "\"If it's not one thing, it's another.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0f0c20c-184e-4d27-ae8b-933abb6fee0c.jpg?1562533013"
    }
}
