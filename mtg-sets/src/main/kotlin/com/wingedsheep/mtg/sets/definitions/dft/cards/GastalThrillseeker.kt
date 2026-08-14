package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Gastal Thrillseeker — Aetherdrift #205
 * {B}{R} · Creature — Lizard Berserker · 2/3
 *
 * Start your engines!
 * When this creature enters, it deals 1 damage to target opponent and you gain 1 life.
 * Max speed — This creature has deathtouch and haste.
 *
 * The ETB is a nice self-contained speed enabler: the damage is dealt *by this creature*
 * (`damageSource = Self`, so lifelink/damage-replacement riders on it apply), and an opponent losing
 * life during your turn is what advances your speed — so casting it on your own turn immediately
 * ticks you from 1 to 2. `startYourEngines()` is keyword-only by design; raising speed to 1 is a
 * state-based action, not a trigger.
 *
 * The max-speed half is the plain keyword-grant shape: two gated [com.wingedsheep.sdk.scripting.GrantKeyword]
 * statics that appear and disappear with your speed, re-evaluated every projection.
 */
val GastalThrillseeker = card("Gastal Thrillseeker") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Lizard Berserker"
    power = 2
    toughness = 3
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "When this creature enters, it deals 1 damage to target opponent and you gain 1 life.\n" +
        "Max speed — This creature has deathtouch and haste."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            Effects.DealDamage(1, opponent, damageSource = EffectTarget.Self),
            Effects.GainLife(1),
        )
        description = "When this creature enters, it deals 1 damage to target opponent and you gain 1 life."
    }

    maxSpeed {
        keywords(Keyword.DEATHTOUCH, Keyword.HASTE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8e5205d-d734-4292-a3c8-70cf5f131289.jpg?1783907857"
    }
}
