package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Starfall Invocation
 * {3}{W}{W}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * Destroy all creatures. If the gift was promised, return a creature card put
 * into your graveyard this way to the battlefield under your control.
 */
val StarfallInvocation = card("Starfall Invocation") {
    manaCost = "{3}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nDestroy all creatures. If the gift was promised, return a creature card put into your graveyard this way to the battlefield under your control."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — just destroy all creatures
            Mode.noTarget(
                Effects.DestroyAll(GameObjectFilter.Creature),
                "Don't promise a gift — destroy all creatures"
            ),
            // Mode 2: Gift a card — opponent draws, destroy all creatures, then return one of yours
            Mode.noTarget(
                CompositeEffect(listOf(
                    DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                    Effects.DestroyAll(GameObjectFilter.Creature, storeDestroyedAs = "destroyed"),
                    SelectFromCollectionEffect(
                        from = "destroyed",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        filter = GameObjectFilter.Creature.ownedByYou(),
                        storeSelected = "returned",
                        prompt = "Choose a creature card put into your graveyard this way to return to the battlefield"
                    ),
                    MoveCollectionEffect(
                        from = "returned",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                    ),
                    Effects.GiftGiven()
                )),
                "Promise a gift — an opponent draws a card, then destroy all creatures and return one of yours to the battlefield"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Rob Rey"
        flavorText = "Nothing lurks in the purity of the moon's light."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2aea38e6-ec58-4091-b27c-2761bdd12b13.jpg?1721425972"
        ruling("2024-07-26", "If the gift was promised, you choose which creature card to return to the battlefield while Starfall Invocation is resolving. No player may take any actions between the time you choose the card and the time you return it.")
    }
}
