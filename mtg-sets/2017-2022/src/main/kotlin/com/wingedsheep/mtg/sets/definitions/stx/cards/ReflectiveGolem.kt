package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reflective Golem — Strixhaven: School of Mages #257 (canonical printing)
 * {3} · Artifact Creature — Golem · 2/3
 *
 * Whenever you cast an instant or sorcery spell that targets only this creature, you may pay {2}.
 * If you do, copy that spell. You may choose new targets for the copy.
 *
 * Mirrorwing Dragon's [SpellCastPredicate.TargetsOnlySource] cast trigger, scoped to spells you
 * cast, paying off with Mendicant Core's [Effects.MayPay] → [Effects.CopyTargetSpell] on the
 * triggering spell (the copy may be retargeted, CR 707.10c).
 */
val ReflectiveGolem = card("Reflective Golem") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast an instant or sorcery spell that targets only this creature, you " +
        "may pay {2}. If you do, copy that spell. You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.TargetsOnlySource)
        )
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
        )
        description = "Whenever you cast an instant or sorcery spell that targets only this creature, " +
            "you may pay {2}. If you do, copy that spell. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "257"
        artist = "Daniel Ljunggren"
        flavorText = "\"We don't cut through Sedgemoor anymore. Can't get five steps without a groff " +
            "trying to fight its own reflection.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c01770c4-8848-495d-8af4-26152d45e9d3.jpg?1783927279"
    }
}
