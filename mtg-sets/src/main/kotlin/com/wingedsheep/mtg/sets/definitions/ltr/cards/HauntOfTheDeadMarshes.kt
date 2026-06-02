package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.dsl.Effects

/**
 * Haunt of the Dead Marshes
 * {B}
 * Creature — Nightmare Elf
 * 1/1
 *
 * When this creature enters, scry 1.
 * {2}{B}: Return this card from your graveyard to the battlefield tapped. Activate only if you
 * control a legendary creature.
 */
val HauntOfTheDeadMarshes = card("Haunt of the Dead Marshes") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Elf"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, scry 1.\n{2}{B}: Return this card from your graveyard to the battlefield tapped. Activate only if you control a legendary creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(1)
    }

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{2}{B}"))
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.legendary())
            )
        )
        effect = Effects.Move(
            target = EffectTarget.Self,
            destination = Zone.BATTLEFIELD,
            placement = ZonePlacement.Tapped
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Miklós Ligeti"
        flavorText = "\"You should not look in when the candles are lit.\"\n—Gollum"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/673c6c12-2513-4d1b-acf0-6a1f741d49dd.jpg?1686968519"
    }
}
