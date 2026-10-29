package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hollowhenge Overlord — Innistrad: Crimson Vow Commander #36
 * {4}{G}{G} · Creature — Wolf · 4/4
 *
 * Flash
 * At the beginning of your upkeep, for each creature you control that's a Wolf or a Werewolf,
 * create a 2/2 green Wolf creature token.
 *
 * The count is one battlefield tally over `withAnyOfSubtypes(Wolf, Werewolf)`, taken on
 * resolution — a creature that's both is one creature, so it's counted once (its ruling), and the
 * Overlord, itself a Wolf, counts itself.
 */
val HollowhengeOverlord = card("Hollowhenge Overlord") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 4
    toughness = 4
    oracleText = "Flash\n" +
        "At the beginning of your upkeep, for each creature you control that's a Wolf or a " +
        "Werewolf, create a 2/2 green Wolf creature token."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)),
            ).count(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf(Subtype.WOLF.value),
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d5f1e139-3054-4273-8a4d-faaaa9c383a8.jpg?1783924694",
        )
        description = "At the beginning of your upkeep, for each creature you control that's a Wolf " +
            "or a Werewolf, create a 2/2 green Wolf creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Milivoj Ćeran"
        flavorText = "The ruins of the town were a notable upgrade from the wolves' forest den."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dac9c33c-bff1-491a-b369-ad92395283a5.jpg?1783924994"
        ruling(
            "2021-11-19",
            "If you control a creature that is both a Wolf and a Werewolf, it's only counted once " +
                "for Hollowhenge Overlord's triggered ability."
        )
    }
}
