package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bristlebud Farmer
 * {2}{G}{G}
 * Creature — Plant Druid
 * 5/5
 *
 * Trample
 * When this creature enters, create two Food tokens.
 * Whenever this creature attacks, you may sacrifice a Food. If you do, mill three cards. You may
 * put a permanent card from among them into your hand.
 *
 * The attack ability is an optional sacrifice gating a mill-and-recur ([MayEffect] over
 * `Sacrifice(Food).then(...)`, the same shape as [com.wingedsheep.mtg.sets.definitions.eoe.cards.LarvalScoutlander]):
 * declining (or having no Food) skips the rest, and on a sacrifice the milled three cards are
 * gathered into a named collection so the optional "put a permanent card from among them into your
 * hand" select can pull from exactly those cards (the Cache Grab mill-and-recur idiom).
 */
val BristlebudFarmer = card("Bristlebud Farmer") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Druid"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "When this creature enters, create two Food tokens. (They're artifacts with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever this creature attacks, you may sacrifice a Food. If you do, mill three cards. You " +
        "may put a permanent card from among them into your hand."

    keywords(Keyword.TRAMPLE)

    // When this creature enters, create two Food tokens.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood(2)
    }

    // Whenever this creature attacks, you may sacrifice a Food. If you do, mill three cards.
    // You may put a permanent card from among them into your hand.
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayEffect(
            Effects.Sacrifice(
                GameObjectFilter.Any.withSubtype("Food"),
                count = 1,
                target = EffectTarget.Controller
            ).then(
                Effects.Composite(
                    listOf(
                        // Mill three cards.
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                            storeAs = "milled"
                        ),
                        MoveCollectionEffect(
                            from = "milled",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD)
                        ),
                        // You may put a permanent card from among them into your hand.
                        SelectFromCollectionEffect(
                            from = "milled",
                            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                            filter = GameObjectFilter.Permanent,
                            storeSelected = "toHand",
                            showAllCards = true,
                            prompt = "You may put a permanent card into your hand",
                            selectedLabel = "Put in hand",
                            remainderLabel = "Leave in graveyard"
                        ),
                        MoveCollectionEffect(
                            from = "toHand",
                            destination = CardDestination.ToZone(Zone.HAND)
                        )
                    )
                )
            )
        )
        description = "You may sacrifice a Food. If you do, mill three cards. You may put a " +
            "permanent card from among them into your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Adrián Rodríguez Pérez"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d498c4de-5e80-4baa-9fcb-70f164880c84.jpg?1739804206"
    }
}
