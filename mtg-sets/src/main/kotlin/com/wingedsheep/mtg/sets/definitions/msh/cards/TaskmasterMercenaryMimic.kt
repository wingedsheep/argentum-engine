package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Taskmaster, Mercenary Mimic — Marvel Super Heroes #232 (rare)
 * {2}{U}{B} · Legendary Creature — Human Mercenary Villain · 3/5
 *
 * Photographic Reflexes — At the beginning of your first main phase, until your next turn,
 * Taskmaster becomes a copy of up to one target creature on the battlefield or creature card in a
 * graveyard, except his name is Taskmaster, Mercenary Mimic and he's a legendary Human Mercenary
 * Villain creature.
 *
 * The same shape as Absorbing Man — [Effects.EachPermanentBecomesCopyOfTarget] with
 * `affected = Self`, `Duration.UntilYourNextTurn`, and the "except" clause in one
 * [CopyExceptions] — with two differences that matter:
 *
 *  - The copy source may be a **card in a graveyard**, so the target is a cross-zone union
 *    (`TargetFilter.or`, the Sorceress's Schemes shape) and the effect needs `sourceFromAnyZone`
 *    to read copiable characteristics outside the battlefield.
 *  - His type clause **replaces** rather than adds. Absorbing Man says "in addition to his other
 *    types" and Taskmaster deliberately does not. CR 205.1a is the default — a stated card type or
 *    subtype replaces the existing one — and CR 205.1b is the exception that retains the prior
 *    types, keyed on exactly the phrase Taskmaster lacks. So `overrideCardTypes` /
 *    `overrideSubtypes`, not the `added*` fields. Copying a Goblin Wizard makes a Human Mercenary
 *    Villain, not a Goblin Wizard Human Mercenary Villain, and copying an artifact creature drops
 *    the artifact type. Only the
 *    legendary supertype is *added*: the clause says nothing about the snow or world supertypes,
 *    and a copy that keeps them is the conservative reading.
 *
 * `nameOverride` keeps him Taskmaster, Mercenary Mimic, so the legend rule (CR 704.5j) still binds
 * against another Taskmaster rather than against whatever he copied. As with Absorbing Man the
 * "until your next turn" window is what lets the trigger fire again: the copy wipes his printed
 * text, and the revert lands after his controller's untap step — before the first main phase.
 */
val TaskmasterMercenaryMimic = card("Taskmaster, Mercenary Mimic") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Mercenary Villain"
    power = 3
    toughness = 5
    oracleText = "Photographic Reflexes — At the beginning of your first main phase, until your " +
        "next turn, Taskmaster becomes a copy of up to one target creature on the battlefield or " +
        "creature card in a graveyard, except his name is Taskmaster, Mercenary Mimic and he's a " +
        "legendary Human Mercenary Villain creature."

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        val copySource = target(
            "up to one target creature on the battlefield or creature card in a graveyard",
            TargetObject(
                count = 1,
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature)
                    .or(TargetFilter(GameObjectFilter.Creature, zone = Zone.GRAVEYARD)),
            ),
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = copySource,
            affected = EffectTarget.Self,
            duration = Duration.UntilYourNextTurn,
            sourceFromAnyZone = true,
            exceptions = CopyExceptions(
                nameOverride = "Taskmaster, Mercenary Mimic",
                addedSupertypes = setOf(Supertype.LEGENDARY),
                overrideCardTypes = setOf(CardType.CREATURE),
                overrideSubtypes = setOf(Subtype.HUMAN, Subtype.MERCENARY, Subtype.VILLAIN),
            ),
        )
        description = "Photographic Reflexes — At the beginning of your first main phase, until " +
            "your next turn, Taskmaster becomes a copy of up to one target creature on the " +
            "battlefield or creature card in a graveyard, except his name is Taskmaster, " +
            "Mercenary Mimic and he's a legendary Human Mercenary Villain creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Riccardo Federici"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d4265fd-cbfa-4e57-89fd-a1d757acfe81.jpg?1783902897"
    }
}
