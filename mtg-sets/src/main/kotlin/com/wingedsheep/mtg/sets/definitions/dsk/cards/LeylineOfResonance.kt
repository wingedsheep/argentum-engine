package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.leyline
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leyline of Resonance (DSK #143)
 * {2}{R}{R}  Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * Whenever you cast an instant or sorcery spell that targets only a single creature you control,
 * copy that spell. You may choose new targets for the copy.
 *
 * "Targets only a single creature you control" is composed from two existing primitives rather
 * than a bespoke predicate: the cast trigger requires [SpellCastPredicate.TargetsMatching] a
 * creature you control (at least one chosen target is a creature you control), and
 * [Conditions.TriggeringSpellHasSingleTarget] requires the spell have exactly one target — together
 * that is "the spell's one and only target is a creature you control." The payoff is the standard
 * [Effects.CopyTargetSpell] of the triggering spell ([EffectTarget.TriggeringEntity]), which already
 * offers "you may choose new targets for the copy." `leyline()` adds the opening-hand marker
 * (CR 103.6).
 */
val LeylineOfResonance = card("Leyline of Resonance") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "Whenever you cast an instant or sorcery spell that targets only a single creature you control, " +
        "copy that spell. You may choose new targets for the copy."

    leyline()

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl()))
        )
        triggerCondition = Conditions.TriggeringSpellHasSingleTarget
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92c5f0e3-345a-40a8-9cda-565a62156692.jpg?1731574465"

        ruling("2024-09-20", "The copy will have the same targets (all the same creature you control) as the spell it's copying unless you choose new ones. You may change any number of the targets, including all of them or none of them. The new targets must be legal.")
        ruling("2024-09-20", "If the spell that's copied is modal, the copy will have the same mode or modes. You can't choose different ones.")
        ruling("2024-09-20", "If the spell that's copied has an X whose value was determined as it was cast, the copy will have the same value of X.")
        ruling("2024-09-20", "The copy is created on the stack, so it's not \"cast.\"")
    }
}
