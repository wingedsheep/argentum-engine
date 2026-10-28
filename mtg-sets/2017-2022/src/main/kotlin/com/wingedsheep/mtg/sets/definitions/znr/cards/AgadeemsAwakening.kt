package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Agadeem's Awakening {X}{B}{B}{B} // Agadeem, the Undercrypt — Zendikar Rising #90
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Agadeem's Awakening: "Return from your graveyard to the battlefield any number of target
 * creature cards that each have a different mana value X or less."
 * Back — Agadeem, the Undercrypt: "As this land enters, you may pay 3 life. If you don't, it enters
 * tapped. {T}: Add {B}."
 *
 * One `unlimited` target slot over creature cards in your graveyard with mana value at most the
 * announced X (X is chosen before targets, CR 601.2b/601.2c), with
 * `differentNumericProperty = MANA_VALUE` keeping the chosen cards on pairwise-distinct mana values.
 * The returned cards enter simultaneously in one move.
 */
private val AgadeemsAwakeningFront = card("Agadeem's Awakening") {
    manaCost = "{X}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return from your graveyard to the battlefield any number of target creature cards " +
        "that each have a different mana value X or less."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature.manaValueAtMostX().ownedByYou(), zone = Zone.GRAVEYARD),
            unlimited = true,
            differentNumericProperty = CardNumericProperty.MANA_VALUE,
        )
        effect = Effects.Pipeline {
            val creatures = gather(CardSource.ChosenTargets)
            move(creatures, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "90"
        artist = "Dmitry Burmak"
        flavorText = "\"Now is the death-hour, just before dawn. Wake, sleepers, and haunt the living!\"\n—Vivias, Witch Vessel"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67f4c93b-080c-4196-b095-6a120a221988.jpg?1783929388"
        ruling("2020-09-25", "For example, if X is 3, you may target up to four creature cards: one with mana value 3, one with mana value 2, one with mana value 1, and one with mana value 0.")
        ruling("2020-09-25", "If X is 0 for Agadeem's Awakening, you can target one creature card with mana value 0.")
        ruling("2020-09-25", "If a card in a player's graveyard has {X} in its mana cost, X is considered to be 0 for that card.")
    }
}

private val AgadeemTheUndercryptBack = card("Agadeem, the Undercrypt") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "90"
        artist = "Dmitry Burmak"
        flavorText = "\"Here below the hedron fields, souls and secrets lie entombed.\"\n—Vivias, Witch Vessel"
        imageUri = "https://cards.scryfall.io/normal/back/6/7/67f4c93b-080c-4196-b095-6a120a221988.jpg?1783929388"
    }
}

val AgadeemsAwakening: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = AgadeemsAwakeningFront,
    backFace = AgadeemTheUndercryptBack,
)
