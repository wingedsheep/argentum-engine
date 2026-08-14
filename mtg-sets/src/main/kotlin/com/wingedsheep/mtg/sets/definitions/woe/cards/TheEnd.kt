package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The End — Wilds of Eldraine #87
 * {2}{B}{B} · Instant
 *
 * The target's controller is snapshotted before the target leaves the battlefield. This matters
 * when the permanent is controlled by someone other than its owner.
 */
val TheEnd = card("The End") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if your life total is 5 or less.\n" +
        "Exile target creature or planeswalker. Search its controller's graveyard, hand, and library " +
        "for any number of cards with the same name as that permanent and exile them. That player " +
        "shuffles, then draws a card for each card exiled from their hand this way."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.LifeAtMost(5))
        )
    }

    spell {
        target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            val target = gather(CardSource.ChosenTargets, name = "target")
            val targetControllers = captureControllers(target, name = "targetControllers")
            val targetName = storeCardName(target, name = "targetName")
            move(target, CardDestination.ToZone(Zone.EXILE))

            forEachCaptured(target, target, targetControllers) {
                val matches = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = Player.You,
                        filter = GameObjectFilter.Any.namedFromVariable(targetName)
                    ),
                    name = "matches"
                )
                val selected = chooseAnyNumber(
                    from = matches,
                    chooser = Chooser.SourceController,
                    prompt = "Choose any number of matching cards to exile",
                    showAllCards = true,
                    alwaysPrompt = true,
                    name = "selected"
                )
                val selectedFromHand = filter(
                    selected,
                    CollectionFilter.InZone(Zone.HAND),
                    name = "selectedFromHand"
                )
                exile(selected)
                run(ShuffleLibraryEffect(target = EffectTarget.Controller))
                run(
                    Effects.DrawCards(
                        DynamicAmount.VariableReference("${selectedFromHand.key}_count"),
                        EffectTarget.Controller
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b18402dc-c4ab-417c-92d1-5e4d9cfb840d.jpg?1783915109"
        ruling(
            "2023-09-01",
            "If the permanent that's exiled was the back face of a double-faced card, you will not be " +
                "able to exile any additional cards, because those cards have only their front-face " +
                "characteristics (including name) in the graveyard, hand, and library."
        )
    }
}
