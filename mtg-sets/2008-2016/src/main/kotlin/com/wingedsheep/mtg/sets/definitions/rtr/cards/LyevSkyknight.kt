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
 * Lyev Skyknight
 * {1}{W}{U}
 * Creature — Human Knight
 * 3/1
 * Flying
 * When this creature enters, detain target nonland permanent an opponent controls. (Until your next turn,
 * that permanent can't attack or block and its activated abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Detain is spelled out as in Azorius Arrester.
 */
val LyevSkyknight = card("Lyev Skyknight") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, detain target nonland permanent an opponent controls. (Until your " +
        "next turn, that permanent can't attack or block and its activated abilities can't be activated.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.CantAttackOrBlock(permanent, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = permanent,
                duration = Duration.UntilYourNextTurn,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11cbeb3b-1579-4318-a024-4a2c06896eaf.jpg?1783940337"
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
