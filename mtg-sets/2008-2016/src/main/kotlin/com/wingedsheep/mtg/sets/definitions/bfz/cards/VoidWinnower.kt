package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Void Winnower
 * {9}
 * Creature — Eldrazi
 * 11/9
 * Your opponents can't cast spells with even mana values. (Zero is even.)
 * Your opponents can't block with creatures with even mana values.
 *
 * Two reused statics. The cast lock is [PlayersCantCastSpells] scoped to each opponent and judged
 * on the spell as proposed: an X spell counts the X chosen for it, and a face-down spell (a morph)
 * has mana value 0, so it can't be cast. The blocking lock is [CantBlock] over the creatures
 * opponents control with an even mana value; a face-down creature has mana value 0 (no mana cost)
 * and an {X} permanent counts X as 0, so both are stopped from blocking.
 */
val VoidWinnower = card("Void Winnower") {
    manaCost = "{9}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 11
    toughness = 9
    oracleText = "Your opponents can't cast spells with even mana values. (Zero is even.)\n" +
        "Your opponents can't block with creatures with even mana values."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.manaValueIsEven(),
        )
    }

    staticAbility {
        ability = CantBlock(GroupFilter(GameObjectFilter.Creature.opponentControls().manaValueIsEven()))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "17"
        artist = "Chase Stone"
        flavorText = "\"Have you ever seen a world fall to its knees? Watch, and learn.\"\n—Ob Nixilis"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cbedb0a-34ca-4d42-bb43-cbea0f3c6d02.jpg?1783938221"
        ruling("2015-08-25", "Effects that increase or reduce the cost to cast a spell don't affect that spell's mana value.")
        ruling("2015-08-25", "For spells with {X} in their mana costs, use the value chosen for X to determine if the spell's mana value is even or not. For example, your opponent could cast Endless One (with mana cost {X}) with X equal to 5, but not with X equal to 6.")
        ruling("2015-08-25", "For creatures on the battlefield with {X} in their mana costs, the value for X is 0. Mistcutter Hydra (a creature with mana cost {X}{G}) could block, but Endless One could not.")
        ruling("2015-08-25", "The mana value of a face-down creature spell or a face-down creature is 0. Void Winnower will stop face-down creature spells from being cast and face-down creatures from blocking.")
    }
}
