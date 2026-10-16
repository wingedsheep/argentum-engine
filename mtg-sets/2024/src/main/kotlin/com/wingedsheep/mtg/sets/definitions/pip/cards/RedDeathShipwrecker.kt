package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Red Death, Shipwrecker — Fallout #116
 * {U}{R} · Legendary Creature — Crab Mutant · 1/3
 *
 * Alluring Eyes — {T}: Goad target creature an opponent controls. That player draws a card.
 * You add {R}.
 *
 * - The ability targets, so it is not a mana ability (CR 605.1a): it uses the stack, and the {R}
 *   is added when it resolves. "Alluring Eyes" is a flavor word with no rules meaning.
 * - "That player" is the goaded creature's controller, read off the target at resolution.
 */
val RedDeathShipwrecker = card("Red Death, Shipwrecker") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Crab Mutant"
    power = 1
    toughness = 3
    oracleText = "Alluring Eyes — {T}: Goad target creature an opponent controls. That player draws a card. " +
        "You add {R}. (Until your next turn, that creature attacks each combat if able and attacks a " +
        "player other than you if able.)"

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Goad(creature) then
            Effects.DrawCards(1, EffectTarget.TargetController) then
            Effects.AddMana(Color.RED)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Jeremy Paillotin"
        flavorText = "\"That little fella is deadly in his own fashion.\"\n—The Mariner"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edf616fa-b9a0-4f7a-b7ee-32ce3a08d466.jpg?1783912387"
        ruling("2024-03-08", "If the target creature is an illegal target as Red Death’s ability tries to resolve, it won’t resolve and none of its effects happen. You won’t add any mana, and the creature’s controller won’t draw a card.")
        ruling("2024-03-08", "Red Death’s ability isn’t a mana ability even though it causes you to add mana. It uses the stack and can be responded to.")
        ruling("2024-03-08", "If, during a player’s declare attackers step, a creature that player controls that’s been goaded is tapped, is affected by a spell or ability that says it can’t attack, or hasn’t been under that player’s control continuously since the turn began (and doesn’t have haste), then it doesn’t attack. If there’s a cost associated with having a creature attack a player, its controller isn’t forced to pay that cost, so it doesn’t have to attack that player.")
        ruling("2024-03-08", "If the creature doesn’t meet any of the above exceptions and can attack, it must attack a player other than the controller of the spell or ability that goaded it if able. If the creature can’t attack any of those players but could otherwise attack, it must attack a planeswalker an opponent controls, a battle an opponent protects, or the player that goaded it.")
        ruling("2024-03-08", "Being goaded isn’t an ability the creature has. Once it’s been goaded, it must attack as detailed above even if it loses all abilities.")
        ruling("2024-03-08", "Attacking with a goaded creature doesn’t cause it to stop being goaded. If there is an additional combat phase that turn, or if another player gains control of it before it stops being goaded, it must attack again if able.")
    }
}
