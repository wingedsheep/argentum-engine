package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Oko, Lorwyn Liege // Oko, Shadowmoor Scion
 * {2}{U} (front)
 * Legendary Planeswalker — Oko (transform)
 * Starting Loyalty: 3
 *
 * Front — Oko, Lorwyn Liege
 *   At the beginning of your first main phase, you may pay {G}. If you do, transform Oko.
 *   +2: Up to one target creature gains all creature types. (This effect doesn't end.)
 *   +1: Target creature gets -2/-0 until your next turn.
 *
 * Back — Oko, Shadowmoor Scion
 *   At the beginning of your first main phase, you may pay {U}. If you do, transform Oko.
 *   −1: Mill three cards. You may put a permanent card from among them into your hand.
 *   −3: Create two 3/3 green Elk creature tokens.
 *   −6: Choose a creature type. You get an emblem with "Creatures you control of the chosen
 *        type get +3/+3 and have vigilance and hexproof."
 */
private val OkoShadowmoorScion = card("Oko, Shadowmoor Scion") {
    manaCost = ""
    typeLine = "Legendary Planeswalker — Oko"
    startingLoyalty = 3
    oracleText = "At the beginning of your first main phase, you may pay {U}. If you do, transform Oko.\n" +
        "−1: Mill three cards. You may put a permanent card from among them into your hand.\n" +
        "−3: Create two 3/3 green Elk creature tokens.\n" +
        "−6: Choose a creature type. You get an emblem with \"Creatures you control of the chosen " +
        "type get +3/+3 and have vigilance and hexproof.\""

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{U}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    // −1: Mill three cards. You may put a permanent card from among them into your hand.
    loyaltyAbility(-1) {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                    storeAs = "milled"
                ),
                SelectFromCollectionEffect(
                    from = "milled",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Permanent,
                    showAllCards = true,
                    storeSelected = "toHand",
                    storeRemainder = "toGraveyard",
                    prompt = "You may put a permanent card from among them into your hand",
                    selectedLabel = "Put into your hand",
                    remainderLabel = "Mill (graveyard)"
                ),
                MoveCollectionEffect(
                    from = "toHand",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "toGraveyard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
            )
        )
    }

    // −3: Create two 3/3 green Elk creature tokens.
    loyaltyAbility(-3) {
        effect = CreateTokenEffect(
            count = 2,
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elk"),
            imageUri = "https://cards.scryfall.io/normal/front/c/f/cf36b5f2-e699-41c0-ae89-23cd9c414b54.jpg?1767955474"
        )
    }

    // −6: Choose a creature type. You get an emblem with "Creatures you control of the chosen
    //     type get +3/+3 and have vigilance and hexproof."
    loyaltyAbility(-6) {
        effect = CompositeEffect(
            listOf(
                ChooseCreatureTypeEffect,
                Effects.CreatePermanentEmblem(
                    groupFilter = GroupFilter(
                        baseFilter = GameObjectFilter.Creature.youControl(),
                        chosenSubtypeKey = "chosenCreatureType"
                    ),
                    powerBonus = 3,
                    toughnessBonus = 3,
                    grantedKeywords = listOf(Keyword.VIGILANCE.name, Keyword.HEXPROOF.name),
                    emblemDescription = "Creatures you control of the chosen type get +3/+3 and have vigilance and hexproof."
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "61"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/back/1/d/1dab370a-1067-4d94-be1f-10362d4abf5a.jpg?1767734249"
    }
}

private val OkoLorwynLiegeFront = card("Oko, Lorwyn Liege") {
    manaCost = "{2}{U}"
    typeLine = "Legendary Planeswalker — Oko"
    startingLoyalty = 3
    oracleText = "At the beginning of your first main phase, you may pay {G}. If you do, transform Oko.\n" +
        "+2: Up to one target creature gains all creature types. (This effect doesn't end.)\n" +
        "+1: Target creature gets -2/-0 until your next turn."

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{G}"),
            effect = TransformEffect(EffectTarget.Self)
        )
    }

    // +2: Up to one target creature gains all creature types. (This effect doesn't end.)
    // Modeled by granting Changeling — the engine treats Changeling as having every creature type.
    loyaltyAbility(+2) {
        val creature = target("creature", TargetCreature(optional = true))
        effect = Effects.GrantKeyword(Keyword.CHANGELING, creature, Duration.Permanent)
    }

    // +1: Target creature gets -2/-0 until your next turn.
    loyaltyAbility(+1) {
        val creature = target("creature", Targets.Creature)
        effect = ModifyStatsEffect(
            powerModifier = -2,
            toughnessModifier = 0,
            target = creature,
            duration = Duration.UntilYourNextTurn
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "61"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dab370a-1067-4d94-be1f-10362d4abf5a.jpg?1767734249"
    }
}

val OkoLorwynLiege: CardDefinition = OkoLorwynLiegeFront.copy(backFace = OkoShadowmoorScion)
