package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Arm the Cathars
 * {1}{W}{W}
 * Sorcery
 *
 * Until end of turn, target creature gets +3/+3, up to one other target creature gets +2/+2, and
 * up to one other target creature gets +1/+1. Those creatures gain vigilance until end of turn.
 *
 * Three targets (the second and third "other" and optional, per the Mabel's Mettle multi-target
 * idiom): the primary +3/+3, then two [TargetOther] optional creatures at +2/+2 and +1/+1. Each
 * pump is paired with a vigilance grant so a skipped optional target simply confers nothing.
 */
val ArmTheCathars = card("Arm the Cathars") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, target creature gets +3/+3, up to one other target creature " +
        "gets +2/+2, and up to one other target creature gets +1/+1. Those creatures gain " +
        "vigilance until end of turn."

    spell {
        val primary = target("target creature", Targets.Creature)
        // Two distinct optional slots — the names must differ, because target bindings are keyed by
        // name (EffectContext.buildNamedTargets). Reusing one name would make both BoundVariables
        // resolve to the same (last-bound) target, so the middle creature would get no bonus.
        val second = target("up to one other target creature (+2/+2)", TargetOther(TargetCreature(optional = true)))
        val third = target("up to one other target creature (+1/+1)", TargetOther(TargetCreature(optional = true)))
        effect = Effects.ModifyStats(3, 3, primary)
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, primary))
            .then(Effects.ModifyStats(2, 2, second))
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, second))
            .then(Effects.ModifyStats(1, 1, third))
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, third))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Zoltan Boros"
        flavorText = "Faith is a cathar's greatest weapon, but a sword of blessed silver is a " +
            "close second."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2004a20c-e434-4691-8ef2-740846ce6a51.jpg?1782703194"
    }
}
