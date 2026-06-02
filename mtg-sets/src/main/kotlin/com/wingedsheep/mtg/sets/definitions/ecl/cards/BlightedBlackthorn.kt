package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Blighted Blackthorn
 * {4}{B}
 * Creature — Treefolk Warlock
 * 3/7
 *
 * Whenever this creature enters or attacks, you may blight 2. If you do, you draw
 * a card and lose 1 life.
 * (To blight 2, put two -1/-1 counters on a creature you control.)
 */
val BlightedBlackthorn = card("Blighted Blackthorn") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Treefolk Warlock"
    power = 3
    toughness = 7
    oracleText = "Whenever this creature enters or attacks, you may blight 2. If you do, you draw a card " +
        "and lose 1 life. (To blight 2, put two -1/-1 counters on a creature you control.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = blightedBlackthornEffect()
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = blightedBlackthornEffect()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Omar Rayyan"
        flavorText = "It bears a forbidden fruit."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3515531a-45d6-4fe0-96a3-7ca1ce545068.jpg?1767871828"
    }
}

/**
 * Inline blight 2 pipeline using ChooseUpTo(1) so the player can back out at the
 * targeting step (selecting zero creatures = effective cancel of the may).
 * The "If you do" draw + life loss is gated on the "blighted" collection actually
 * containing a creature.
 */
private fun blightedBlackthornEffect(): Effect = MayEffect(
    effect = Effects.Composite(
        listOf(
            GatherCardsEffect(
                source = CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature),
                storeAs = "blightTargets"
            ),
            SelectFromCollectionEffect(
                from = "blightTargets",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "blighted",
                prompt = "Blight 2 — choose a creature you control (or cancel)",
                useTargetingUI = true,
                alwaysPrompt = true
            ),
            AddCountersToCollectionEffect("blighted", Counters.MINUS_ONE_MINUS_ONE, 2),
            ConditionalOnCollectionEffect(
                collection = "blighted",
                ifNotEmpty = Effects.DrawCards(1) then Effects.LoseLife(1)
            )
        )
    ),
    descriptionOverride = "You may blight 2. If you do, you draw a card and lose 1 life."
)
