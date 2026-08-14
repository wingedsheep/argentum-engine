package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Back for Seconds
 * {2}{B}
 * Sorcery
 *
 * Bargain
 * Return up to two target creature cards from your graveyard to your hand. If this spell was
 * bargained, you may put one of those cards with mana value 4 or less onto the battlefield
 * instead of putting it into your hand.
 *
 * Targets are chosen while casting, but the optional battlefield choice happens on resolution
 * after target legality is checked. Moving the chosen eligible card first and then filtering the
 * original target collection to cards still in the graveyard implements "instead"; the selected
 * card cannot subsequently be moved to hand by the final step.
 */
val BackForSeconds = card("Back for Seconds") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Return up to two target creature cards from your graveyard to your hand. If this spell " +
        "was bargained, you may put one of those cards with mana value 4 or less onto the " +
        "battlefield instead of putting it into your hand."

    bargain()

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard,
        )
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.ChosenTargets,
                storeAs = "backForSecondsTargets",
            ),
            ConditionalEffect(
                condition = Conditions.WasBargained,
                effect = Effects.Composite(
                    SelectFromCollectionEffect(
                        from = "backForSecondsTargets",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        storeSelected = "backForSecondsReanimated",
                        filter = GameObjectFilter.Creature.manaValueAtMost(4),
                        prompt = "Put up to one creature card with mana value 4 or less onto the battlefield",
                    ),
                    MoveCollectionEffect(
                        from = "backForSecondsReanimated",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                        underOwnersControl = true,
                    ),
                    FilterCollectionEffect(
                        from = "backForSecondsTargets",
                        filter = CollectionFilter.InZone(Zone.GRAVEYARD),
                        storeMatching = "backForSecondsToHand",
                    ),
                    MoveCollectionEffect(
                        from = "backForSecondsToHand",
                        destination = CardDestination.ToZone(Zone.HAND),
                    ),
                ),
                elseEffect = MoveCollectionEffect(
                    from = "backForSecondsTargets",
                    destination = CardDestination.ToZone(Zone.HAND),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/660845b5-96fa-4484-822b-aa0508801306.jpg?1783915111"

        ruling(
            "2023-09-01",
            "If you bargain this spell, you can still choose not to put one of the cards onto the " +
                "battlefield, even if at least one is eligible.",
        )
    }
}
