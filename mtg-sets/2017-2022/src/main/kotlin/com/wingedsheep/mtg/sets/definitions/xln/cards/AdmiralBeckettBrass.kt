package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Admiral Beckett Brass
 * {1}{U}{B}{R}
 * Legendary Creature — Human Pirate
 * 3/3
 * Other Pirates you control get +1/+1.
 * At the beginning of your end step, gain control of target nonland permanent controlled by a
 * player who was dealt combat damage by three or more Pirates this turn.
 *
 * The target restriction reads the target's controller's per-turn record of combat-damage sources
 * (`controllerMeets` binds "you" to that controller): each Pirate counts as it was when it dealt
 * the damage, even if it has since left or stopped being a Pirate, and a double striker counts once.
 * The control change lasts indefinitely.
 */
val AdmiralBeckettBrass = card("Admiral Beckett Brass") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Pirate"
    oracleText = "Other Pirates you control get +1/+1.\n" +
        "At the beginning of your end step, gain control of target nonland permanent controlled by a " +
        "player who was dealt combat damage by three or more Pirates this turn."
    power = 3
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.PIRATE).youControl(),
                excludeSelf = true,
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val permanent = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.controllerMeets(
                    Conditions.PlayerWasDealtCombatDamageThisTurnBy(
                        GameObjectFilter.Creature.withSubtype(Subtype.PIRATE),
                        atLeast = 3,
                    )
                )
            )
        )
        effect = Effects.GainControl(permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "217"
        artist = "Jason Rainville"
        flavorText = "\"You and your ship will make a fine addition to my fleet.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9854c2ec-0e53-4ac7-b417-554a9331c5e0.jpg?1783935715"
    }
}
