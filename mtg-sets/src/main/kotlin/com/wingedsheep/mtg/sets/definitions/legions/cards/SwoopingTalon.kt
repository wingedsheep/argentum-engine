package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swooping Talon
 * {4}{W}{W}
 * Creature — Bird Soldier
 * 2/6
 * Flying
 * {1}: This creature loses flying until end of turn.
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 */
val SwoopingTalon = card("Swooping Talon") {
    manaCost = "{4}{W}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 6
    oracleText = "Flying\n{1}: This creature loses flying until end of turn.\nProvoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)"

    keywords(Keyword.FLYING, Keyword.PROVOKE)

    // {1}: This creature loses flying until end of turn.
    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.RemoveKeyword(Keyword.FLYING, EffectTarget.Self, Duration.EndOfTurn)
    }

    // Provoke
    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34c3d19c-d4c6-4c5c-85eb-11d55959a89c.jpg?1562905654"
    }
}
