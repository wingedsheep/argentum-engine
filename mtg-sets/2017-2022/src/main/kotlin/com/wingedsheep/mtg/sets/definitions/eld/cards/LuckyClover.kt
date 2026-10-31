package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lucky Clover
 * {2}
 * Artifact
 *
 * Whenever you cast an Adventure instant or sorcery spell, copy it. You may choose new targets
 * for the copy.
 *
 * "An Adventure spell" is how the card was cast ([SpellCastPredicate.CastAsAdventure]), and the
 * stack object carries only the Adventure face's characteristics (CR 715.3b), so the
 * instant-or-sorcery filter reads the face's type line. The copy is the standard
 * [Effects.CopyTargetSpell] on the triggering spell (mandatory — no "may"), which offers the
 * retarget step (CR 707.10c). A copy isn't a card: it ceases to exist as it leaves the stack, so
 * only the original card goes on an adventure.
 */
val LuckyClover = card("Lucky Clover") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you cast an Adventure instant or sorcery spell, copy it. " +
        "You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts(
            spell = GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.CastAsAdventure)
        )
        effect = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "John Stanko"
        flavorText = "Syr Branigan always remembered to kiss his good luck charm, having lost his " +
            "foot to wolverines the last time he forgot."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b5d23a6-3a23-4169-aea1-f10bf5153180.jpg?1783932584"
        ruling(
            "2019-10-04",
            "If an effect copies an Adventure spell, that copy is exiled as it resolves. It ceases " +
                "to exist as a state-based action; it's not possible to cast the copy as a creature."
        )
        ruling(
            "2019-10-04",
            "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a " +
                "player casts a spell won't trigger."
        )
        ruling(
            "2019-10-04",
            "Lucky Clover's ability and the copy it creates both resolve before the Adventure " +
                "spell. They resolve even if the Adventure spell is countered before the copy is created."
        )
    }
}
