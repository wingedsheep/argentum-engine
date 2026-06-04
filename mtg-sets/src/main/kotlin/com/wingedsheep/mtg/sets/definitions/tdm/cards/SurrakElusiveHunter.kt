package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Surrak, Elusive Hunter — Tarkir: Dragonstorm #161
 * {2}{G} · Legendary Creature — Human Warrior · Rare
 * 4/3
 *
 * This spell can't be countered.
 * Trample
 * Whenever a creature you control or a creature spell you control becomes the target of a spell
 * or ability an opponent controls, draw a card.
 *
 * Both halves of the trigger ("a creature you control" — a permanent — and "a creature spell you
 * control" — a spell on the stack) reduce to the same shape: a creature object you control that an
 * opponent's spell/ability targets. The engine now emits a [BecomesTargetEvent] for spell targets
 * as well as permanent targets, so the single
 * [Triggers.CreatureYouControlBecomesTargetByOpponent] handles both. The creature filter matches a
 * creature spell via its type line, and the spell's controller falls back to its caster.
 */
val SurrakElusiveHunter = card("Surrak, Elusive Hunter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 3
    oracleText = "This spell can't be countered.\n" +
        "Trample\n" +
        "Whenever a creature you control or a creature spell you control becomes the target of a " +
        "spell or ability an opponent controls, draw a card."

    cantBeCountered = true

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.CreatureYouControlBecomesTargetByOpponent(
            GameObjectFilter.Creature,
            includeSpellTargets = true
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4775a26-0b66-40e9-8f64-41d9308ca032.jpg?1743204609"
    }
}
