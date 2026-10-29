package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Qasali Slingers — Commander 2017 #33
 * {4}{G} · Creature — Cat Warrior · 3/5
 *
 * Reach
 * Whenever this creature or another Cat you control enters, you may destroy target artifact or
 * enchantment.
 *
 * Qasali Slingers is itself a Cat, so one "a Cat you control enters" trigger covers both halves
 * (the Nebelgast Herald shape); several Cats entering together trigger it once each.
 */
val QasaliSlingers = card("Qasali Slingers") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Warrior"
    power = 3
    toughness = 5
    oracleText = "Reach\n" +
        "Whenever this creature or another Cat you control enters, you may destroy target artifact or enchantment."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.CAT).youControl()).enters()
        optional = true
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
        description = "Whenever this creature or another Cat you control enters, you may destroy target " +
            "artifact or enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Izzy"
        flavorText = "All things will crumble, whether by time or by force."
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40e21394-146e-4648-b81e-63659c0c4764.jpg?1783935939"
        ruling(
            "2017-08-25",
            "If Qasali Slingers enters the battlefield at the same time as other Cats you control, its " +
                "ability will trigger for each of those Cats."
        )
    }
}
