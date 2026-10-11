package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Harmonic Prodigy — Modern Horizons 2 #132
 * {1}{R} · Creature — Human Wizard · 1/3 · Rare
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * If a triggered ability of a Shaman or another Wizard you control triggers, that ability
 * triggers an additional time.
 *
 * Modeling notes:
 *  - "another" binds only to the Wizard leg: a Shaman Harmonic Prodigy (e.g. via a type-changing
 *    effect) would double its own triggers. So `excludeSelf = false`, and the self-exclusion lives
 *    inside the Wizard branch as `notSourceItself()`, which the trigger detector evaluates with
 *    the doubler as `sourceId`. The heterogeneous `or` builds an `anyOf` union so the
 *    `notSourceItself` gate does not leak onto the Shaman branch.
 *  - "you control" is enforced by [AdditionalSourceTriggers] itself (it only doubles triggers
 *    controlled by the doubler's controller).
 */
val HarmonicProdigy = card("Harmonic Prodigy") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "If a triggered ability of a Shaman or another Wizard you control triggers, that ability " +
        "triggers an additional time."

    prowess()

    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = GameObjectFilter.Permanent.withSubtype(Subtype.SHAMAN) or
                GameObjectFilter.Permanent.withSubtype(Subtype.WIZARD).notSourceItself(),
            excludeSelf = false,
            description = "If a triggered ability of a Shaman or another Wizard you control " +
                "triggers, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Paul Scott Canavan"
        flavorText = "Each note strikes with deadly precision."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22579ac0-ad3f-4000-a65a-46a17a7f1aa5.jpg?1783926842"
    }
}
