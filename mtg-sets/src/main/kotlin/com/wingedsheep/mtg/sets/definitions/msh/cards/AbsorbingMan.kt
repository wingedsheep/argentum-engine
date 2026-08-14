package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
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
 * Absorbing Man — Marvel Super Heroes #199 (rare)
 * {1}{G}{U} · Legendary Creature — Human Villain · 4/4
 *
 * Vigilance
 * At the beginning of your first main phase, until your next turn, Absorbing Man becomes a copy of
 * up to one target artifact, non-Aura enchantment, or land, except his name is Absorbing Man, he's
 * a legendary 4/4 Human Villain creature in addition to his other types, and he has vigilance.
 *
 * The whole card is one copy effect with a long "except" clause (CR 707.9b), so it is
 * [Effects.EachPermanentBecomesCopyOfTarget] with `affected = Self` and every modification in one
 * [CopyExceptions]:
 *  - `nameOverride` keeps him named Absorbing Man, so two of him still meet the legend rule and no
 *    "target Sol Ring" effect finds him under the copied name.
 *  - `addedSupertypes` / `addedCardTypes` / `addedSubtypes` are the "**in addition to** his other
 *    types" direction (CR 205.1b makes that phrase retain the prior types, against CR 205.1a's
 *    replace-by-default): copying a land leaves him a land that is *also* a legendary Human
 *    Villain creature.
 *  - `powerOverride` / `toughnessOverride` give the 4/4. The copy source is a noncreature, so there
 *    are no copied base stats to modify — the applier creates them.
 *  - `addedKeywords` re-grants vigilance, which the copy would otherwise have wiped along with the
 *    rest of his printed text.
 *
 * `Duration.UntilYourNextTurn` is what makes the card loop. Copying replaces his card component
 * wholesale, so while the copy is up this very trigger is gone; the copy is reverted after the
 * untap step of his controller's next turn, which is before the first main phase, so the trigger
 * is back in time to fire and pick a new object every turn.
 *
 * The target is optional ("up to one"), and a target that has become illegal simply leaves the copy
 * effect a no-op — Absorbing Man stays himself for the turn.
 */
val AbsorbingMan = card("Absorbing Man") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Villain"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "At the beginning of your first main phase, until your next turn, Absorbing Man becomes a " +
        "copy of up to one target artifact, non-Aura enchantment, or land, except his name is " +
        "Absorbing Man, he's a legendary 4/4 Human Villain creature in addition to his other " +
        "types, and he has vigilance."
    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        val copySource = target(
            "up to one target artifact, non-Aura enchantment, or land",
            TargetObject(
                count = 1,
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.Artifact or
                        GameObjectFilter.Enchantment.notSubtype(Subtype.AURA) or
                        GameObjectFilter.Land
                ),
            ),
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = copySource,
            affected = EffectTarget.Self,
            duration = Duration.UntilYourNextTurn,
            exceptions = CopyExceptions(
                nameOverride = "Absorbing Man",
                addedKeywords = setOf(Keyword.VIGILANCE),
                addedSupertypes = setOf(Supertype.LEGENDARY),
                addedCardTypes = setOf(CardType.CREATURE),
                addedSubtypes = setOf(Subtype.HUMAN, Subtype.VILLAIN),
                powerOverride = 4,
                toughnessOverride = 4,
            ),
        )
        description = "At the beginning of your first main phase, until your next turn, Absorbing " +
            "Man becomes a copy of up to one target artifact, non-Aura enchantment, or land, " +
            "except his name is Absorbing Man, he's a legendary 4/4 Human Villain creature in " +
            "addition to his other types, and he has vigilance."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Nathaniel Himawan"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7114ef87-53ff-43e9-864c-9faa455d86ef.jpg?1783902907"
    }
}
