package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Mercurial Geists — Eldritch Moon #186. */
val MercurialGeists = card("Mercurial Geists") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\n" +
        "Whenever you cast an instant or sorcery spell, this creature gets +3/+0 until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(3, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Lake Hurwitz"
        flavorText = "\"Procedural misstep: provocation of geists for the purpose of yielding more " +
            "energy can compromise containment units. Next action: clean up mess.\"\n—Laboratory notes"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ab14779-7e79-449c-9a47-6ba219d0dbc5.jpg"
    }
}
