package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Pippin's Bravery
 * {G}
 * Instant
 *
 * You may sacrifice a Food. If you do, target creature gets +4/+4 until end of turn.
 * Otherwise, that creature gets +2/+2 until end of turn.
 *
 * Sacrificing a Food is a resolution-time optional action, not a cost, so it's modeled with an
 * [IfYouDoEffect] over a Gather → choose-up-to-1 → sacrifice pipeline (cf. Heated Argument), not an
 * OptionalCostEffect cost-gate. The optional `ChooseUpTo(1)` is the "you may"; gating on
 * `SuccessCriterion.CollectionNonEmpty` over the *chosen* pile (rather than the move's destination)
 * is what distinguishes +4/+4 (a Food was chosen and sacrificed) from +2/+2 (none was — declined,
 * or none controlled, in which case there's nothing to choose so no prompt appears).
 */
val PippinsBravery = card("Pippin's Bravery") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "You may sacrifice a Food. If you do, target creature gets +4/+4 until end of turn. Otherwise, that creature gets +2/+2 until end of turn."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = IfYouDoEffect(
            action = Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Any.withSubtype("Food"),
                            player = Player.You
                        ),
                        storeAs = "controlledFood"
                    ),
                    SelectFromCollectionEffect(
                        from = "controlledFood",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        storeSelected = "sacrificedFood",
                        useTargetingUI = true,
                        prompt = "You may sacrifice a Food",
                        selectedLabel = "Sacrifice"
                    ),
                    MoveCollectionEffect(
                        from = "sacrificedFood",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD),
                        moveType = MoveType.Sacrifice
                    )
                )
            ),
            ifYouDo = Effects.ModifyStats(4, 4, creature),
            ifYouDont = Effects.ModifyStats(2, 2, creature),
            successCriterion = SuccessCriterion.CollectionNonEmpty("sacrificedFood", min = 1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "John Tedrick"
        flavorText = "Pippin stabbed upwards, and the written blade of Westernesse pierced through the hide and went deep into the vitals of the troll."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc60dc65-6813-4d57-877b-df195ed00d00.jpg?1686969536"
    }
}
