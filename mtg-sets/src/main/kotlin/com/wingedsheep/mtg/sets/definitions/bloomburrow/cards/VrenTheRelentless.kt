package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker

/**
 * Vren, the Relentless
 * {2}{U}{B}
 * Legendary Creature — Rat Rogue
 * 3/4
 *
 * Ward {2}
 * If a creature an opponent controls would die, exile it instead.
 * At the beginning of each end step, create X 1/1 black Rat creature tokens with
 * "This creature gets +1/+1 for each other Rat you control," where X is the number
 * of creatures that were exiled under your opponents' control this turn.
 */
val VrenTheRelentless = card("Vren, the Relentless") {
    manaCost = "{2}{U}{B}"
    typeLine = "Legendary Creature — Rat Rogue"
    power = 3
    toughness = 4
    oracleText = "Ward {2}\nIf a creature an opponent controls would die, exile it instead.\n" +
        "At the beginning of each end step, create X 1/1 black Rat creature tokens with " +
        "\"This creature gets +1/+1 for each other Rat you control,\" where X is the number " +
        "of creatures that were exiled under your opponents' control this turn."

    // Ward {2} — keyword display only; ward mechanic not yet enforced by engine
    keywords(Keyword.WARD)

    // Replacement effect: if a creature an opponent controls would die, exile it instead
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                to = Zone.GRAVEYARD
            )
        )
    )

    // At the beginning of each end step, create X Rat tokens
    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = CreateTokenEffect(
            count = DynamicAmount.TurnTracking(Player.You, TurnTracker.OPPONENT_CREATURES_EXILED),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rat"),
            imageUri = "https://cards.scryfall.io/normal/front/1/c/1c0977b2-3342-4b7e-b1c7-f06bd8ab7fbf.jpg?1721428982",
            staticAbilities = listOf(
                GrantDynamicStatsEffect(
                    target = StaticTarget.SourceCreature,
                    powerBonus = DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.Creature.withSubtype(Subtype("Rat")),
                        excludeSelf = true
                    ),
                    toughnessBonus = DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.Creature.withSubtype(Subtype("Rat")),
                        excludeSelf = true
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6506277d-f031-4db5-9d16-bf2389094785.jpg?1721427225"
        ruling(
            "2024-07-26",
            "While Vren is on the battlefield, creatures your opponents control are exiled instead of dying, " +
                "and abilities that trigger when a creature dies won't trigger."
        )
        ruling(
            "2024-07-26",
            "If Vren would leave the battlefield at the same time as one or more creatures an opponent controls " +
                "would die, those creatures will still be exiled."
        )
        ruling(
            "2024-07-26",
            "Cards that would go to your opponent's graveyard for reasons other than dying, such as being " +
                "discarded or milled, will still go to the graveyard and will not be exiled instead."
        )
        ruling(
            "2024-07-26",
            "Vren's last ability counts any creatures your opponents controlled that were exiled this turn, " +
                "including creature tokens."
        )
    }
}
