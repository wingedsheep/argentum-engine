package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Identity Thief — Eldritch Moon #64
 * {2}{U}{U} · Creature — Shapeshifter · 0/3
 *
 * Whenever this creature attacks, you may exile another target nontoken creature. If you do, this
 * creature becomes a copy of that creature until end of turn. Return the exiled card to the
 * battlefield under its owner's control at the beginning of the next end step.
 *
 * Niko, Light of Hope's composition behind Astral Slide's "you may":
 *  1. [Patterns.Exile.exileUntilEndStep] exiles the target and schedules the delayed "return it at
 *     the beginning of the next end step" trigger under its owner's control.
 *  2. [Effects.EachPermanentBecomesCopyOfTarget] with `affected = Self` makes Identity Thief the
 *     copy. The target left the battlefield earlier in this same resolution, so the executor reads
 *     its copiable values as it last existed there (CR 608.2h, CR 707.2) — the face shown, and what
 *     it was copying if it was a copy — never its counters or non-copy effects. `sourceFromAnyZone`
 *     is the fallback that lets the exiled source be read at all.
 *  3. [Duration.EndOfTurn] keeps the copy through the end step the card returns in (ruling) and
 *     drops it at cleanup.
 *
 * Copying replaces only the copiable values, so Identity Thief stays tapped and attacking, keeps
 * counters and earlier non-copy effects. The "may" sits over all three steps: declining exiles
 * nothing and copies nothing. A target gone by resolution fizzles the whole trigger.
 */
val IdentityThief = card("Identity Thief") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 3
    oracleText = "Whenever this creature attacks, you may exile another target nontoken creature. " +
        "If you do, this creature becomes a copy of that creature until end of turn. Return the " +
        "exiled card to the battlefield under its owner's control at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter(GameObjectFilter.Creature.nontoken()).other())
        effect = Effects.May(
            Patterns.Exile.exileUntilEndStep(creature) then
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = creature,
                    affected = EffectTarget.Self,
                    duration = Duration.EndOfTurn,
                    sourceFromAnyZone = true,
                ),
            prompt = "Exile the target creature and have Identity Thief become a copy of it until end of turn?",
        )
        description = "Whenever this creature attacks, you may exile another target nontoken " +
            "creature. If you do, this creature becomes a copy of that creature until end of turn. " +
            "Return the exiled card to the battlefield under its owner's control at the beginning " +
            "of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9865100f-dddc-407e-a379-2c285475ec89.jpg?1783937499"
        ruling(
            "2016-07-13",
            "If the targeted creature has an ability that triggers when it attacks, it'll be too " +
                "late for it to trigger once Identity Thief has that ability. The same is true if " +
                "another object has an ability that triggers when a creature with certain " +
                "characteristics attacks and Identity Thief doesn't have those characteristics " +
                "until after it becomes a copy."
        )
        ruling(
            "2016-07-13",
            "Identity Thief copies the printed values of the targeted creature, plus any copy " +
                "effects that have been applied to that creature. It won't copy other effects that " +
                "have changed the targeted creature's power, toughness, types, color, or so on. It " +
                "also won't copy counters on the targeted creature."
        )
        ruling(
            "2016-07-13",
            "If the targeted creature is a double-faced or melded permanent, Identity Thief copies " +
                "the face shown. Because Identity Thief is not a double-faced card or meld card, it " +
                "won't be able to transform or meld. If it copies a melded permanent or the back " +
                "face of a double-faced card, its mana value will be 0. Exiled double-faced cards " +
                "and exiled meld cards will return with their front faces up."
        )
        ruling(
            "2016-07-13",
            "If the targeted creature is itself copying a permanent, Identity Thief will become " +
                "whatever it's copying, as modified by that copy effect. For example, if you target " +
                "a Hamlet Captain that has become a copy of Permeating Mass, Identity Thief will " +
                "become a Permeating Mass."
        )
        ruling(
            "2016-07-13",
            "This effect can cause Identity Thief to stop being a creature. For example, if you " +
                "target an animated Gideon Jura (a planeswalker with a loyalty ability that turns " +
                "him into a creature), only the printed wording will be copied; the \"becomes a " +
                "creature\" effect won't. Identity Thief will become a noncreature Gideon with no " +
                "loyalty, be removed from combat, and then be put into its owner's graveyard."
        )
        ruling(
            "2016-07-13",
            "This effect can cause Identity Thief to become a copy of a creature that can't " +
                "normally attack, such as Lupine Prototype or a creature with defender. In these " +
                "cases, it remains attacking."
        )
        ruling(
            "2016-07-13",
            "Noncopy effects that have already applied to Identity Thief will continue to apply to " +
                "it. For example, if Woodcutter's Grit gave Identity Thief +3/+3 and hexproof " +
                "earlier in the turn, Identity Thief will still have those bonuses after copying a " +
                "creature."
        )
        ruling(
            "2016-07-13",
            "Identity Thief will still be a copy of the creature during the next end step when the " +
                "exiled card returns. If it has an ability that triggers at the beginning of the end " +
                "step, that ability will trigger for Identity Thief and not for the returned creature."
        )
        ruling(
            "2016-07-13",
            "Auras attached to the exiled creature will be put into their owners' graveyards. Any " +
                "Equipment will become unattached and remain on the battlefield. Any counters on " +
                "the exiled permanent will cease to exist."
        )
    }
}
