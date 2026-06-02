package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kheru Lich Lord
 * {3}{B}{G}{U}
 * Creature — Zombie Wizard
 * 4/4
 * At the beginning of your upkeep, you may pay {2}{B}. If you do, return a creature card
 * at random from your graveyard to the battlefield. It gains flying, trample, and haste.
 * Exile that card at the beginning of your next end step. If it would leave the battlefield,
 * exile it instead of putting it anywhere else.
 */
val KheruLichLord = card("Kheru Lich Lord") {
    manaCost = "{3}{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Creature — Zombie Wizard"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your upkeep, you may pay {2}{B}. If you do, return a creature card " +
            "at random from your graveyard to the battlefield. It gains flying, trample, and haste. " +
            "Exile that card at the beginning of your next end step. If it would leave the battlefield, " +
            "exile it instead of putting it anywhere else."

    val returned = EffectTarget.PipelineTarget("returned")

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}{B}"),
            effect = Effects.Composite(
                listOf(
                    // Gather all creature cards from your graveyard
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                        storeAs = "creatures"
                    ),
                    // Select one at random
                    SelectFromCollectionEffect(
                        from = "creatures",
                        selection = SelectionMode.Random(DynamicAmount.Fixed(1)),
                        storeSelected = "chosen"
                    ),
                    // Move to battlefield
                    MoveCollectionEffect(
                        from = "chosen",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                        storeMovedAs = "returned"
                    ),
                    // Grant flying, trample, and haste (permanent — no duration specified in oracle text)
                    GrantKeywordEffect(Keyword.FLYING, returned, Duration.Permanent),
                    GrantKeywordEffect(Keyword.TRAMPLE, returned, Duration.Permanent),
                    GrantKeywordEffect(Keyword.HASTE, returned, Duration.Permanent),
                    // Exile at the beginning of your next end step
                    CreateDelayedTriggerEffect(
                        step = Step.END,
                        effect = Effects.Move(returned, Zone.EXILE)
                    ),
                    // If it would leave the battlefield, exile it instead
                    Effects.GrantExileOnLeave(returned)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7fbca8d-34a3-4df1-986a-36fca142a758.jpg?1562795282"
        ruling("2014-09-20", "You decide whether to pay {2}{B} as the ability resolves.")
        ruling("2014-09-20", "The creature card returned to the battlefield is chosen at random as the ability resolves.")
        ruling("2014-09-20", "If a creature card returned to the battlefield with Kheru Lich Lord would leave the battlefield for any reason, it's exiled instead.")
        ruling("2014-09-20", "The exiled creature is never put into the graveyard. Any abilities the creature has that trigger when it dies won't trigger.")
    }
}
