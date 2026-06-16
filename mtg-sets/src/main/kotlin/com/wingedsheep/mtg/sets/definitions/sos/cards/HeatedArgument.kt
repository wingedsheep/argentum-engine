package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Heated Argument
 * {4}{R}
 * Instant
 * Heated Argument deals 6 damage to target creature. You may exile a card from your graveyard.
 * If you do, Heated Argument also deals 2 damage to that creature's controller.
 *
 * The optional graveyard exile is modeled as a [MayEffect] + [IfYouDoEffect] gate: gather your
 * graveyard, choose exactly one card, exile it, and only deal the 2 extra damage when a card was
 * actually exiled ([SuccessCriterion.CollectionNonEmpty] on the moved pile). With an empty graveyard
 * the player can't complete the exile, so the rider never happens. The rider's damage hits
 * [EffectTarget.TargetController] — the controller of the targeted creature — and is dealt by this
 * spell ("Heated Argument also deals ..."), matching the targeted creature's damage source.
 */
val HeatedArgument = card("Heated Argument") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Heated Argument deals 6 damage to target creature. You may exile a card from your " +
        "graveyard. If you do, Heated Argument also deals 2 damage to that creature's controller."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(6, creature) then MayEffect(
            IfYouDoEffect(
                action = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(zone = Zone.GRAVEYARD),
                            storeAs = "graveyardCards",
                        ),
                        SelectFromCollectionEffect(
                            from = "graveyardCards",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            storeSelected = "toExile",
                            selectedLabel = "Exile",
                        ),
                        MoveCollectionEffect(
                            from = "toExile",
                            destination = CardDestination.ToZone(Zone.EXILE),
                        ),
                    ),
                ),
                ifYouDo = Effects.DealDamage(2, EffectTarget.TargetController),
                successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 1),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Aleksi Briclot"
        flavorText = "\"I don't want to hurt you, Ajani, but whatever Jace is planning needs to be stopped!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0038d212-3d95-4f98-8c2e-7b2404d0ced7.jpg"
    }
}
