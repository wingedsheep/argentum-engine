package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lambholt Elder // Silverpelt Werewolf (Dark Ascension #122)
 *
 * The Innistrad werewolf transform pair (Hinterland Hermit's shape); the back's card draw is the
 * plain self combat-damage-to-a-player trigger (Stealer of Secrets).
 */
private val LambholtElderFront = card("Lambholt Elder") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 1
    toughness = 2
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(),
            ComparisonOperator.EQ,
            0,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Matt Stewart"
        flavorText = "\"Be wary of the seemingly gentle souls. The weak here were slaughtered long ago.\"\n" +
            "—Rem Karolus, Blade of the Inquisitors"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/932d753d-9584-4ad8-9a5e-a3524184f961.jpg?1783940809"
    }
}

private val SilverpeltWerewolf = card("Silverpelt Werewolf") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 5
    oracleText = "Whenever this creature deals combat damage to a player, draw a card.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/back/9/3/932d753d-9584-4ad8-9a5e-a3524184f961.jpg?1783940809"
    }
}

val LambholtElder: CardDefinition =
    CardDefinition.doubleFacedCreature(LambholtElderFront, SilverpeltWerewolf)
