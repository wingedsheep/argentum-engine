package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gelatinous Cube
 * {2}{B}{B}
 * Creature — Ooze
 * 4/3
 * Engulf — When this creature enters, exile target non-Ooze creature an opponent controls until
 * this creature leaves the battlefield.
 * Dissolve — {X}{B}: Put target creature card with mana value X exiled with this creature into its
 * owner's graveyard.
 *
 * Engulf is [Effects.MoveUntilSourceLeaves]: a one-shot exile with a duration (no return trigger),
 * which does nothing if the Cube is already gone when it resolves, and links the exiled card to
 * the Cube. Dissolve targets that linked pile (`exiledWithSource()` in exile) with the X paid for
 * the activation threaded into the target filter (`manaValueEqualsX()`). A card Dissolve moves to
 * the graveyard is a new object, so the Cube leaving later no longer returns it.
 */
val GelatinousCube = card("Gelatinous Cube") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ooze"
    oracleText = "Engulf — When this creature enters, exile target non-Ooze creature an opponent controls " +
        "until this creature leaves the battlefield.\n" +
        "Dissolve — {X}{B}: Put target creature card with mana value X exiled with this creature into " +
        "its owner's graveyard."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(
            TargetFilter(GameObjectFilter.Creature.opponentControls().notSubtype(Subtype.OOZE))
        )
        effect = Effects.MoveUntilSourceLeaves(victim, Zone.EXILE)
    }

    activatedAbility {
        cost = Costs.Mana("{X}{B}")
        val engulfed = target(
            TargetFilter(GameObjectFilter.Creature.exiledWithSource().manaValueEqualsX(), zone = Zone.EXILE)
        )
        effect = Effects.Move(engulfed, Zone.GRAVEYARD, fromZone = Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c472e70-300c-4881-86e7-c5ca690ab9b6.jpg?1783926493"
        ruling(
            "2021-07-23",
            "If Gelatinous Cube leaves the battlefield before its Engulf ability resolves, the target " +
                "non-Ooze creature won't be exiled."
        )
        ruling(
            "2021-07-23",
            "Auras attached to the exiled creature will be put into their owners' graveyards. Any Equipment " +
                "will become unattached and remain on the battlefield. Any counters on the exiled creature will " +
                "cease to exist. When the card returns to the battlefield, it will be a new object with no " +
                "connection to the card that was exiled."
        )
        ruling(
            "2021-07-23",
            "If a token is exiled this way, it will cease to exist and won't return to the battlefield."
        )
        ruling(
            "2021-07-23",
            "If the engulfed creature card leaves exile, perhaps because of the Dissolve ability, it won't " +
                "return to the battlefield after Gelatinous Cube leaves the battlefield."
        )
    }
}
