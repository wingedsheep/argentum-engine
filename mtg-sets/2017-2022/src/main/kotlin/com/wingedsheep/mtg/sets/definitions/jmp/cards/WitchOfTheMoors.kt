package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Witch of the Moors
 * {3}{B}{B}
 * Creature — Human Warlock
 * 4/4
 * Deathtouch
 * At the beginning of your end step, if you gained life this turn, each opponent sacrifices a
 * creature of their choice and you return up to one target creature card from your graveyard to
 * your hand.
 *
 * An intervening-if end-step trigger ([Conditions.YouGainedLifeThisTurn], Restless Bloodseeker's
 * gate) whose body is Dictate of Erebos's each-opponent sacrifice followed by Courier Bat's
 * optional graveyard-to-hand return. Choosing no target is legal; a chosen target that has become
 * illegal fizzles the whole ability, so no one sacrifices (2020-06-23 ruling).
 */
val WitchOfTheMoors = card("Witch of the Moors") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 4
    toughness = 4
    oracleText = "Deathtouch\n" +
        "At the beginning of your end step, if you gained life this turn, each opponent sacrifices " +
        "a creature of their choice and you return up to one target creature card from your " +
        "graveyard to your hand."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurn
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard, optional = true)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.Move(creatureCard, Zone.HAND)
        description = "At the beginning of your end step, if you gained life this turn, each opponent " +
            "sacrifices a creature of their choice and you return up to one target creature card from " +
            "your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Caio Monteiro"
        flavorText = "Many hands for many evils."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f484f19a-0121-4173-a70b-6698cc5f6303.jpg?1783930504"
        ruling("2020-06-23", "The triggered ability triggers just once, no matter how much life you've gained.")
        ruling("2020-06-23", "You don't have to choose a target creature card at all. If you do and the target card is an illegal target by the time the ability tries to resolve, the ability won't resolve. Your opponents won't sacrifice creatures.")
        ruling("2020-06-23", "If you haven't gained life by the time your end step begins, the triggered ability won't trigger at all.")
        ruling("2020-06-23", "The triggered ability looks at whether you've gained life in the turn, even if Witch of the Moors wasn't on the battlefield when you gained life. It doesn't care if you also lost life, even if you lost more life than you gained.")
    }
}
