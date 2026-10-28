package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Umara Mystic — Zendikar Rising #238
 * {1}{U}{R} · Creature — Merfolk Wizard · 1/3
 *
 * Flying
 * Whenever you cast an instant, sorcery, or Wizard spell, this creature gets +2/+0 until end of turn.
 */
val UmaraMystic = card("Umara Mystic") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Flying\n" +
        "Whenever you cast an instant, sorcery, or Wizard spell, this creature gets +2/+0 until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery or GameObjectFilter.Any.withSubtype(Subtype.WIZARD)
        )
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "Whenever you cast an instant, sorcery, or Wizard spell, this creature gets +2/+0 " +
            "until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Bryan Sola"
        flavorText = "\"We tolerate the North Hada thieves' den, but cutthroats who wander downriver earn a " +
            "skymanta ride out to sea.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb979cfc-3e5b-42c9-b78d-584c274ed912.jpg?1783929318"
        ruling("2020-09-25", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger, but after targets have been chosen for that spell (if it has any targets). The ability resolves even if that spell is countered.")
        ruling("2020-09-25", "A Wizard spell is one with the creature type Wizard. Spells that are Wizard-themed (such as Relic Amulet) aren't Wizard spells.")
    }
}
