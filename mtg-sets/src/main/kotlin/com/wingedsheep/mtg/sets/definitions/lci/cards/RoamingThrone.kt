package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Roaming Throne
 * {4}
 * Artifact Creature — Golem
 * 4/4
 *
 * Ward {2}
 * As this creature enters, choose a creature type.
 * This creature is the chosen type in addition to its other types.
 * If a triggered ability of another creature you control of the chosen type triggers,
 * it triggers an additional time.
 *
 * Composed entirely from existing primitives:
 *  - **Ward {2}** — [KeywordAbility.Ward] + [WardCost.Mana].
 *  - **As it enters, choose a creature type** — [EntersWithChoice] with [ChoiceType.CREATURE_TYPE]
 *    stores the chosen type on the permanent's `CastChoicesComponent` (CR 614.12 as-it-enters choice).
 *  - **Is the chosen type in addition** — [GrantChosenSubtype] (default filter: the source itself)
 *    adds the chosen type to Roaming Throne in Layer 4.
 *  - **Doubles chosen-type creatures' triggers** — [AdditionalSourceTriggers] (same primitive as
 *    Twinflame Travelers' "another Elemental you control") with a chosen-subtype filter:
 *    `GameObjectFilter.Creature.withChosenSubtype()` matches creatures whose projected subtypes
 *    include the type chosen on *this* permanent (via `CardPredicate.HasChosenSubtype`, read from the
 *    doubler's `sourceId`). `excludeSelf = true` honours "another" — Roaming Throne, though it becomes
 *    the chosen type itself, doesn't double its own triggers. The "you control" restriction is enforced
 *    by the engine's doubler pass, which only doubles triggers whose controller matches the doubler's
 *    controller. The duplicated trigger is a full copy — a "may"/targeted ability offers an independent
 *    second choice — and multiple Roaming Thrones sharing a chosen type each add an instance (additive).
 */
val RoamingThrone = card("Roaming Throne") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 4
    toughness = 4
    oracleText = "Ward {2}\n" +
        "As this creature enters, choose a creature type.\n" +
        "This creature is the chosen type in addition to its other types.\n" +
        "If a triggered ability of another creature you control of the chosen type triggers, it triggers an additional time."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    // As this creature enters, choose a creature type.
    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    // This creature is the chosen type in addition to its other types.
    staticAbility {
        ability = GrantChosenSubtype()
    }

    // If a triggered ability of another creature you control of the chosen type triggers,
    // it triggers an additional time.
    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = GameObjectFilter.Creature.withChosenSubtype(),
            excludeSelf = true,
            description = "If a triggered ability of another creature you control of the chosen type triggers, it triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32fd8b7c-baf3-4d3d-be6f-044a917b11a0.jpg?1782694404"
    }
}
