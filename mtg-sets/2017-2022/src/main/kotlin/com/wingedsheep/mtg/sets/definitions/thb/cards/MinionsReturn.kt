package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Minion's Return
 * {2}{B}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When enchanted creature dies, return that card to the battlefield under your control.
 *
 * Necrogen Communion's return: an attached-bound dies trigger moving the triggering card out of the
 * graveyard under the Aura controller's control — any creature, so an opponent's creature comes back
 * as yours. A token ceases to exist in the graveyard and so returns nothing (the ruling).
 */
val MinionsReturn = card("Minion's Return") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When enchanted creature dies, return that card to the battlefield under your control."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
        description = "When enchanted creature dies, return that card to the battlefield under your control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Billy Christian"
        flavorText = "As a mortal, Phenax was the first to escape the Underworld. As a god, he brings his pawns " +
            "along the same dark path."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce763778-0a35-41d2-9286-fdf2302554f5.jpg?1783931564"
        ruling(
            "2020-01-24",
            "If Minion's Return enchants a token creature, that creature won't return to the battlefield when it " +
                "dies. Choose your minions wisely."
        )
        ruling(
            "2020-01-24",
            "In a multiplayer game, if a player leaves the game, all cards that player owns leave as well. If you " +
                "leave the game, the creature you control from Minion's Return is exiled if you don't own it."
        )
    }
}
