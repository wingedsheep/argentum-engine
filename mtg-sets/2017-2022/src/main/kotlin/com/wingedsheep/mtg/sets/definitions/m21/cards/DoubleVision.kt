package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Double Vision
 * {3}{R}{R}
 * Enchantment
 *
 * Whenever you cast your first instant or sorcery spell each turn, copy that spell. You may choose
 * new targets for the copy.
 *
 * "Your first instant or sorcery spell each turn" narrows the trigger event, so it is a
 * `triggerRestriction` (the Aquatic Alchemist shape), not an intervening "if". The per-turn count
 * includes spells cast before Double Vision entered, so a first spell cast earlier that turn keeps
 * the ability from triggering at all (2020-06-23 ruling). The payoff is the Leyline of Resonance
 * [Effects.CopyTargetSpell] of the triggering spell, which reads the spell's last-known information
 * if it has already been countered.
 */
val DoubleVision = card("Double Vision") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast your first instant or sorcery spell each turn, copy that spell. " +
        "You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts()
        triggerRestriction = Conditions.YouCastFirstSpellOfTypeThisTurn(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
        description = "Whenever you cast your first instant or sorcery spell each turn, copy that spell. " +
            "You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Heonhwa"
        flavorText = "\"Your eyes aren't playing tricks on you. I am.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/5/754b77b4-4cc4-4e49-a7a4-667401e7e063.jpg?1783930692"
        ruling("2020-06-23", "If you cast an instant or sorcery spell before Double Vision enters the battlefield during the same turn, its ability can't trigger that turn.")
        ruling("2020-06-23", "A copy is created even if the spell that caused Double Vision's ability to trigger has been countered by the time that ability resolves. The copy resolves before the original spell.")
        ruling("2020-06-23", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode or modes. You can't choose different ones.")
        ruling("2020-06-23", "If the spell that's copied has an X whose value was determined as it was cast, the copy has the same value of X.")
        ruling("2020-06-23", "The copy that Double Vision's ability creates is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
    }
}
