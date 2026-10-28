package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Khalni Ambush {2}{G} // Khalni Territory — Zendikar Rising #192
 * Instant // Land · Modal double-faced card
 *
 * Front — Khalni Ambush: "Target creature you control fights target creature you don't control."
 * Back — Khalni Territory: "This land enters tapped. {T}: Add {G}."
 *
 * "You don't control" is `Not(ControlledByYou)` rather than `opponentControls()` — the two agree in a
 * duel and separate in Two-Headed Giant, where a teammate's creature is one you don't control.
 */
private val KhalniAmbushFront = card("Khalni Ambush") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control fights target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)"

    spell {
        val yourCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(
            TargetFilter(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"The difference between life and death in Ora Ondar is about three seconds of " +
            "startled screaming.\"\n—Raff Slugeater, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99535539-aa73-41ed-86ab-21c97b92620d.jpg?1783929343"
        ruling("2020-09-25", "If either target is an illegal target as Khalni Ambush resolves, no creature will deal or be dealt damage.")
    }
}

private val KhalniTerritoryBack = card("Khalni Territory") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped.\n{T}: Add {G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"If you wander into the Khalni Garden without knowing the signs of a scythecat's " +
            "territory, you're not going to live long enough to learn.\"\n—Raff Slugeater, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/back/9/9/99535539-aa73-41ed-86ab-21c97b92620d.jpg?1783929343"
    }
}

val KhalniAmbush: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = KhalniAmbushFront,
    backFace = KhalniTerritoryBack,
)
