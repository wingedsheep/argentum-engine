package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Unbury
 * {1}{B}
 * Instant
 *
 * Choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Return two target creature cards that share a creature type from your graveyard
 *   to your hand.
 *
 * Note: the second mode is implemented as "choose a creature type, then return two
 * creature cards of that type" — same set of legal outcomes as the oracle wording,
 * with one extra player decision (the creature type) instead of inferring it from
 * the chosen pair.
 */
val Unbury = card("Unbury") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return target creature card from your graveyard to your hand.\n" +
        "• Return two target creature cards that share a creature type from your graveyard to your hand."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                TargetObject(filter = TargetFilter.CreatureInYourGraveyard),
                "Return target creature card from your graveyard to your hand"
            ),
            Mode.noTarget(
                Effects.Composite(
                    listOf(
                        ChooseCreatureTypeEffect,
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                zone = Zone.GRAVEYARD,
                                player = Player.You,
                                filter = GameObjectFilter.Creature
                            ),
                            storeAs = "graveyardCreatures"
                        ),
                        SelectFromCollectionEffect(
                            from = "graveyardCreatures",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                            matchChosenCreatureType = true,
                            storeSelected = "chosen",
                            prompt = "Choose two creature cards of the chosen type"
                        ),
                        MoveCollectionEffect(
                            from = "chosen",
                            destination = CardDestination.ToZone(Zone.HAND)
                        )
                    )
                ),
                "Return two creature cards that share a creature type from your graveyard"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Kev Fang"
        flavorText = "A well-aged boggart body is steeped in magics richer than any fae glamer."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b00766db-4109-4225-a62a-fa12fd526970.jpg?1767871924"
        ruling("2025-11-17", "If you choose the second mode, the cards must share at least one creature type, such as Faerie or Goblin. Card types such as artifact, and supertypes such as legendary or snow, aren't creature types.")
        ruling("2025-11-17", "If you choose the second mode and one of the two cards leaves your graveyard, you'll still return the other card to your hand as long as it has a creature type that the other card had as it left your graveyard.")
    }
}
