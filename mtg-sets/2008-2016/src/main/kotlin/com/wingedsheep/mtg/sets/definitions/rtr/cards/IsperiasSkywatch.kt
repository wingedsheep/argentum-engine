package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Isperia's Skywatch
 * {5}{U}
 * Creature — Vedalken Knight
 * 3/3
 * Flying
 * When this creature enters, detain target creature an opponent controls. (Until your next turn,
 * that creature can't attack or block and its activated abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Detain is spelled out as in Azorius Arrester.
 */
val IsperiasSkywatch = card("Isperia's Skywatch") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Knight"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, detain target creature an opponent controls. (Until your " +
        "next turn, that creature can't attack or block and its activated abilities can't be activated.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantAttackOrBlock(creature, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.UntilYourNextTurn,
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/019ba84d-d236-45c5-a8ad-75def7736d0c.jpg?1783940368"
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
