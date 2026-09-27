package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Azgol // Ashen Reaper — March of the Machine #232 (canonical printing).
 * {B}{R} · Battle — Siege · defense 4 // Creature — Zombie Elemental 2/1
 *
 * When this Siege enters, target player sacrifices a creature or planeswalker of their choice and
 * loses 1 life.
 * // Menace. At the beginning of your end step, put a +1/+1 counter on this creature if a
 * //   permanent was put into a graveyard from the battlefield this turn.
 *
 * Ashen Reaper's "if" is not an intervening-if (it's at the end of the sentence, not after the
 * trigger condition), so it is checked on resolution only — an `If` inside the effect. The count is
 * game-wide and counts permanents of every type, tokens included.
 */
private val InvasionOfAzgolFront = card("Invasion of Azgol") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, target player sacrifices a creature or planeswalker of their " +
        "choice and loses 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, target = player) then
            Effects.LoseLife(1, player)
        description = "When this Siege enters, target player sacrifices a creature or planeswalker " +
            "of their choice and loses 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae2e1244-f05b-45f9-8afc-59f190524798.jpg?1783916952"
    }
}

/**
 * The back face. Cast transformed, for free, by the Siege's defeat trigger — so it has no mana
 * cost of its own and needs a colour indicator to be black and red off the battlefield.
 */
private val AshenReaper = card("Ashen Reaper") {
    manaCost = ""
    colorIdentity = "BR"
    colorIndicator = "BR"
    typeLine = "Creature — Zombie Elemental"
    oracleText = "Menace\nAt the beginning of your end step, put a +1/+1 counter on this creature " +
        "if a permanent was put into a graveyard from the battlefield this turn."
    power = 2
    toughness = 1
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.permanentsPutIntoGraveyardFromBattlefieldThisTurn(),
                ComparisonOperator.GT,
                0,
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
        description = "At the beginning of your end step, put a +1/+1 counter on this creature if a " +
            "permanent was put into a graveyard from the battlefield this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Joshua Raphael"
        flavorText = "Eternal hatred fuels eternal fire."
        imageUri = "https://cards.scryfall.io/normal/back/a/e/ae2e1244-f05b-45f9-8afc-59f190524798.jpg?1783916952"
    }
}

val InvasionOfAzgol: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfAzgolFront,
    backFace = AshenReaper,
)
