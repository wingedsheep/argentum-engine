package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Imprisoned in the Moon
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant creature, land, or planeswalker
 * Enchanted permanent is a colorless land with "{T}: Add {C}" and loses all other card types
 * and abilities.
 *
 * Current Oracle wording (verified via Scryfall, not the 2015 Magic Origins printing's original
 * templating): this is a 2017-era functional errata. The original ORI printing read "When
 * Imprisoned in the Moon enters the battlefield, if enchanted permanent isn't a land, it becomes
 * a colorless land named Moon with '{T}: Add {C}' and loses all other card types and abilities" —
 * a ONE-SHOT transform from an ETB trigger with an intervening-if, that (per the old rulings)
 * did NOT revert if the Aura later left the battlefield, and was a no-op on an already-a-land
 * permanent. The current Oracle text below has neither: it's a CONTINUOUS "Enchanted permanent
 * is..." static effect (no ETB trigger, no intervening-if, no "named Moon"), tied to the Aura
 * remaining attached — confirmed by the ruling "If you remove Imprisoned in the Moon from a
 * planeswalker after tapping it for mana, you can still activate a loyalty ability of that
 * planeswalker even though it's tapped" (the planeswalker stops being a land, and its land-ness
 * stops applying, the moment the Aura is removed). It also now applies even when the enchanted
 * permanent is ALREADY a land — see the land-type-retention ruling below.
 *
 * Modeled as a stack of statics on the enchanted permanent, the same shape as Sugar Coat /
 * Witness Protection ("becomes a different thing entirely" via [TransformPermanent]):
 *  - [TransformPermanent] Layer 4 (TYPE) replaces all card types with LAND (`setSubtypes` is empty
 *    and `clearSubtypes` is false on purpose, see the land ruling below: land types stay, while the
 *    engine drops the creature/planeswalker types of the lost card types per CR 205.1a) and
 *    Layer 5 (COLOR) sets colorless.
 *  - [LoseAllAbilities] (Layer 6) strips every other ability — including any intrinsic mana
 *    ability the permanent already had (e.g. a Plains's "{T}: Add {W}").
 *  - [GrantActivatedAbility] (Layer 6) grants "{T}: Add {C}".
 *
 * Rulings (2016-07-13, Eldritch Moon release):
 *  - "If the enchanted permanent is a land and has land types, it retains those types even
 *    though it loses any intrinsic mana abilities associated with them. For example, a Plains
 *    enchanted by Imprisoned in the Moon is still a Plains, but it can't tap for {W}, only for
 *    {C}." — this is why `setSubtypes`/`clearSubtypes` are left at their defaults: land types
 *    survive because LAND is kept (CR 205.1a only drops the subtypes of lost card types).
 *  - "Becoming a land may cause other Auras to become illegally attached. These are put into
 *    their owner's graveyard, and any Equipment attached to the land become unattached and
 *    remain on the battlefield. Counters on the land remain on it even if they don't do
 *    anything." — handled generically by the engine's existing illegal-attachment/SBA checks;
 *    no card-specific code needed.
 *  - "At the time the permanent becomes enchanted, Imprisoned in the Moon causes it to lose all
 *    abilities except the noted mana ability. Any abilities the permanent gains after that point
 *    will work normally." — consistent with [LoseAllAbilities] + [GrantActivatedAbility] being
 *    continuous Layer 6 effects (later, higher-timestamp ability grants apply on top).
 *  - "The permanent will keep any supertypes it previously had. Notably, if Imprisoned in the
 *    Moon is enchanting a legendary permanent, that permanent will continue to be legendary." —
 *    CR 205.4b: changing card types/subtypes never changes supertypes; [TransformPermanent] only
 *    touches `setCardTypes`/`setSubtypes`, so supertypes are untouched automatically.
 *  - "If you remove Imprisoned in the Moon from a planeswalker after tapping it for mana, you
 *    can still activate a loyalty ability of that planeswalker even though it's tapped." —
 *    confirms the transform is continuous (tied to the Aura's presence on the battlefield), not
 *    a one-shot resolution effect.
 *
 * Earliest printing is Eldritch Moon (2016); this is the canonical definition. Foundations
 * (2024) carries only a [com.wingedsheep.sdk.model.Printing] row.
 */
val ImprisonedInTheMoon = card("Imprisoned in the Moon") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature, land, or planeswalker\nEnchanted permanent is a colorless " +
        "land with \"{T}: Add {C}\" and loses all other card types and abilities."

    // Enchant creature, land, or planeswalker
    auraTarget = TargetObject(filter = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsCreature,
                            CardPredicate.IsLand,
                            CardPredicate.IsPlaneswalker
                        )
                    )
                )
            )
        ))

    // "is a colorless land" — Layer 4 (type) + Layer 5 (color). Subtypes deliberately left
    // unchanged (see ruling on Plains keeping its land type).
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("LAND"),
            setColors = emptySet() // colorless
        )
    }

    // "loses all other card types and abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "with '{T}: Add {C}'" — Layer 6
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.AddColorlessMana(1),
                isManaAbility = true,
                descriptionOverride = "{T}: Add {C}."
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Ryan Alexander Lee"
        flavorText = "Only one vault was great enough to hold Emrakul."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7990ebba-e9f2-4ba4-a352-e26ec81d4bed.jpg?1782711902"

        ruling(
            "2016-07-13",
            "If the enchanted permanent is a land and has land types, it retains those types " +
                "even though it loses any intrinsic mana abilities associated with them. For " +
                "example, a Plains enchanted by Imprisoned in the Moon is still a Plains, but " +
                "it can't tap for {W}, only for {C}."
        )
        ruling(
            "2016-07-13",
            "Becoming a land may cause other Auras to become illegally attached. These are put " +
                "into their owner's graveyard, and any Equipment attached to the land become " +
                "unattached and remain on the battlefield. Counters on the land remain on it " +
                "even if they don't do anything."
        )
        ruling(
            "2016-07-13",
            "The permanent will keep any supertypes it previously had. Notably, if Imprisoned " +
                "in the Moon is enchanting a legendary permanent, that permanent will continue " +
                "to be legendary."
        )
        ruling(
            "2016-07-13",
            "If you remove Imprisoned in the Moon from a planeswalker after tapping it for " +
                "mana, you can still activate a loyalty ability of that planeswalker even " +
                "though it's tapped."
        )
    }
}
