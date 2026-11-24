package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inaction Injunction
 * {1}{U}
 * Sorcery
 *
 * Detain target creature an opponent controls. (Until your next turn, that creature can't attack
 * or block and its activated abilities can't be activated.)
 * Draw a card.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Detain is spelled out as in Azorius Arrester: can't attack or block until your next turn, plus a
 * granted activation ban for the same duration; then an untargeted draw.
 */
val InactionInjunction = card("Inaction Injunction") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Detain target creature an opponent controls. (Until your next turn, that creature can't " +
        "attack or block and its activated abilities can't be activated.)\nDraw a card."

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.UntilYourNextTurn,
            ) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Wayne Reynolds"
        flavorText = "\"To prevent action is to prevent transgression.\"\n—Azorius Arrester motto"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5342ec3c-9d26-474a-9df5-c21ac90bb233.jpg?1783940369"
        ruling(
            "2013-04-15",
            "Activated abilities include a colon and are written in the form \"[cost]: [effect].\" No one can activate any activated abilities, including mana abilities, of a detained permanent."
        )
        ruling("2013-04-15", "The static abilities of a detained permanent still apply. The triggered abilities of a detained permanent can still trigger.")
        ruling("2013-04-15", "If a creature is already attacking or blocking when it's detained, it won't be removed from combat. It will continue to attack or block.")
        ruling("2013-04-15", "If a permanent's activated ability is on the stack when that permanent is detained, the ability will be unaffected.")
        ruling("2013-04-15", "If a noncreature permanent is detained and later turns into a creature, it won't be able to attack or block.")
        ruling("2013-04-15", "When a player leaves a multiplayer game, any continuous effects with durations that last until that player's next turn or until a specific point in that turn will last until that turn would have begun. They neither expire immediately nor last indefinitely.")
    }
}
