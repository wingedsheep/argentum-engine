package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Artisan of Forms — Theros #40 (canonical printing)
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, you may have this creature become
 * a copy of target creature, except it has this ability.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero); the copy is Protean Thaumaturge's
 * shape — [Effects.EachPermanentBecomesCopyOfTarget] on itself with
 * `retainResolvingTriggeredAbility`, which stamps the resolving heroic trigger onto the copy as
 * copiable rules text, so it keeps re-shaping and anything copying it gets heroic too (ruling).
 * The copy is indefinite (`Duration.Permanent`, the facade default).
 */
val ArtisanOfForms = card("Artisan of Forms") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, you may have this creature " +
        "become a copy of target creature, except it has this ability."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val creature = target(TargetFilter.Creature)
        description = "Heroic — Whenever you cast a spell that targets this creature, you may have this creature " +
            "become a copy of target creature, except it has this ability."
        effect = Effects.May(
            Effects.EachPermanentBecomesCopyOfTarget(
                target = creature,
                affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainResolvingTriggeredAbility = true),
            ),
            sourceRequiredZone = Zone.BATTLEFIELD,
            descriptionOverride = "Have Artisan of Forms become a copy of the target creature?",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7f10198-1eef-4577-b375-b900b15060e1.jpg?1783939802"
        ruling(
            "2013-09-15",
            "You choose the target for the triggered ability when the ability is put onto the stack. You choose " +
                "whether or not Artisan of Forms becomes a copy of that creature when the ability resolves."
        )
        ruling(
            "2013-09-15",
            "Artisan of Forms copies the printed values of the creature plus any copy effects that have been applied " +
                "to it. It won't copy any other effects that have changed that creature's power, toughness, color, " +
                "and so on. Artisan of Forms won't copy any counters on the creature, but Artisan of Forms will " +
                "retain any counters it already had on it."
        )
        ruling(
            "2013-09-15",
            "If Artisan of Forms becomes a copy of a token creature, it copies the original characteristics of that " +
                "token as defined by the effect that put it onto the battlefield. It won't become a token creature."
        )
        ruling(
            "2013-09-15",
            "The copy effect lasts indefinitely. Often, it will last until it is overwritten by another copy effect " +
                "(if it copies another creature on a future turn, perhaps)."
        )
        ruling(
            "2013-09-15",
            "If the creature is an illegal target when the ability tries to resolve, the ability won't resolve. " +
                "Artisan of Forms won't become a copy of that creature. It remains whatever it was a copy of."
        )
        ruling(
            "2013-09-15",
            "When Artisan of Forms becomes a copy of a creature, it's neither entering nor leaving the battlefield. " +
                "Any enters-the-battlefield or leaves-the-battlefield abilities won't trigger."
        )
        ruling(
            "2013-09-15",
            "If another creature becomes a copy of Artisan of Forms, it will become a copy of whatever Artisan of " +
                "Forms is currently copying (if anything), plus it will have the triggered ability."
        )
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
