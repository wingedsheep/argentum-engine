package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deftblade Elite
 * {W}
 * Creature — Human Soldier
 * 1/1
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 * {1}{W}: Prevent all combat damage that would be dealt to and dealt by Deftblade Elite this turn.
 */
val DeftbladeElite = card("Deftblade Elite") {
    manaCost = "{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Provoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)\n{1}{W}: Prevent all combat damage that would be dealt to and dealt by Deftblade Elite this turn."

    keywords(Keyword.PROVOKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.PreventCombatDamageToAndBy(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76ffbae4-7aad-493c-86a0-c6e6425da8fd.jpg?1562918855"
    }
}
