package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Overzealous Muscle
 * {4}{B}
 * Creature — Ogre Mercenary
 * 5/4
 * Whenever you commit a crime during your turn, this creature gains indestructible until end of turn.
 *
 * The "during your turn" clause is an intervening-if (CR 603.4) gated on [Conditions.IsYourTurn] —
 * the crime trigger only fires on your own turn. The payoff grants INDESTRUCTIBLE to this creature
 * until end of turn via [Effects.GrantKeyword] on [EffectTarget.Self]. The crime-this-turn tracker
 * is read at the trigger's emit site by the engine's `CrimeDetector`; this card only consumes
 * [Triggers.YouCommitCrime].
 */
val OverzealousMuscle = card("Overzealous Muscle") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Mercenary"
    power = 5
    toughness = 4
    oracleText = "Whenever you commit a crime during your turn, this creature gains indestructible until end of turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime. " +
        "Damage and effects that say \"destroy\" don't destroy a creature with indestructible.)"

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        triggerCondition = Conditions.IsYourTurn
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self, Duration.EndOfTurn)
        description = "Whenever you commit a crime during your turn, this creature gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Sam White"
        flavorText = "Not everyone can be a pickpocket."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce57d977-e9c5-4ed1-915f-cdc90a54f8f8.jpg"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
    }
}
