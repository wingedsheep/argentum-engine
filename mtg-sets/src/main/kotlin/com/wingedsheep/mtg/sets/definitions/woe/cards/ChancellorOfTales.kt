package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chancellor of Tales
 * {3}{U}
 * Creature — Faerie Advisor
 * 2/3
 *
 * Flying
 * Whenever you cast an Adventure spell, you may copy it. You may choose new targets for the copy.
 *
 * "An Adventure spell" is a cast-time fact, not a characteristic of the card — the same adventurer
 * cast as its creature half doesn't trigger this. That's [SpellCastPredicate.CastAsAdventure]; the
 * copy itself is the standard [Effects.CopyTargetSpell] on the triggering spell, which already
 * offers the "you may choose new targets" step (CR 707.10).
 *
 * The copy is created on the stack rather than cast, so it never re-triggers this ability, and it
 * is exiled as it resolves without granting permission to cast the creature (2023-09-01 ruling).
 */
val ChancellorOfTales = card("Chancellor of Tales") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Advisor"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you cast an Adventure spell, you may copy it. You may choose new targets for the copy."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.youCastSpell(requires = setOf(SpellCastPredicate.CastAsAdventure))
        effect = MayEffect(Effects.CopyTargetSpell(EffectTarget.TriggeringEntity))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Joshua Raphael"
        flavorText = "\"...so the knight stabbed the troll, the troll threw the knight off the " +
            "bridge, and they both forgot about the sword.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f67bd5ef-305b-4bf7-990b-3014778b14a0.jpg?1783915122"
        ruling(
            "2023-09-01",
            "If an effect copies an Adventure spell, that copy is exiled as it resolves. It ceases " +
                "to exist as a state-based action; it's not possible to cast the copy as a permanent."
        )
    }
}
