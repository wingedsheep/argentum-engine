package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thinking Cap — Murders at Karlov Manor #257
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+2.
 * Equip Detective {1}
 * Equip {3}
 *
 * "Equip Detective {1}" is an "Equip [quality]" variant (CR 702.6c) — the Pirate Hat / Dúnedain
 * Blade idiom — authored through the [equipAbility] facade's `quality`/`targetFilter` pair, exactly
 * like the plain "Equip {3}" below it.
 *
 * The two are genuinely separate abilities, not one ability with a discount: a Detective can be
 * equipped either way, and a non-Detective only via the {3} ability. Modelling the Detective clause
 * as a cost reduction on a single equip ability would wrongly let you point the cheap version at a
 * non-Detective (it would just cost more), so the two-ability shape is the faithful one.
 *
 * Unlike this set's murder-weapon Equipment (Knife, Wrench, Rope, …) this one is *not* a Clue — it
 * has no sacrifice-to-draw and the type line carries no Clue subtype.
 */
val ThinkingCap = card("Thinking Cap") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+2.\n" +
        "Equip Detective {1}\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    // Equipped creature gets +1/+2.
    staticAbility {
        ability = ModifyStats(+1, +2, Filters.EquippedCreature)
    }

    // Equip Detective {1}: attach only to a Detective you control, sorcery speed (CR 702.6c).
    equipAbility(
        "{1}",
        quality = "Detective",
        targetFilter = TargetFilter.CreatureYouControl.withSubtype(Subtype.DETECTIVE),
    )

    // Equip {3}: attach to any creature you control, sorcery speed.
    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "257"
        artist = "Tony Foti"
        flavorText = "\"I find that my greatest insights rarely arrive when my ears are cold.\"\n" +
            "—Senior Inspector Holjo"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d2565e1-dd7b-462b-8270-a17913277793.jpg?1783912825"
    }
}
