package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fin Fang Foom — Marvel Super Heroes #129 (rare)
 * {2}{R}{R} · Legendary Creature — Alien Dragon Villain · 3/5
 *
 * Flying
 * Whenever you cast an instant or sorcery spell that targets an artifact or land, copy that spell.
 * You may choose new targets for the copy. Put two +1/+1 counters on Fin Fang Foom.
 *
 * The trigger is [Triggers.youCastSpell] narrowed to [GameObjectFilter.InstantOrSorcery] with a
 * [SpellCastPredicate.TargetsMatching]`(ArtifactOrLand)` cast-time requirement — at least one of the
 * spell's chosen targets is an artifact or a land, of any controller (Leyline of Resonance's shape,
 * minus its single-target rider). The payoff composes the two existing effects: the standard
 * [Effects.CopyTargetSpell] on [EffectTarget.TriggeringEntity], which already offers "you may
 * choose new targets for the copy," then two +1/+1 counters on Fin Fang Foom itself. Both halves
 * happen even if the copy has no legal targets left, since they resolve as one ability.
 */
val FinFangFoom = card("Fin Fang Foom") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Alien Dragon Villain"
    power = 3
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever you cast an instant or sorcery spell that targets an artifact or land, copy " +
        "that spell. You may choose new targets for the copy. Put two +1/+1 counters on Fin Fang " +
        "Foom."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.TargetsMatching(GameObjectFilter.ArtifactOrLand))
        )
        effect = Effects.Composite(
            Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        )
        description = "Whenever you cast an instant or sorcery spell that targets an artifact or " +
            "land, copy that spell. You may choose new targets for the copy. Put two +1/+1 " +
            "counters on Fin Fang Foom."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "129"
        artist = "Filipe Pagliuso"
        flavorText = "His limbs shatter mountains. His back scrapes the sun."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7777fac1-7bf5-4454-960d-26ec3183e392.jpg?1783902932"
    }
}
