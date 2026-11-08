package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withSubtypeFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pol Jamaar, Illusionist — Foundations Jumpstart #38 (canonical printing)
 * {4}{U}{U} · Legendary Creature — Human Illusion Wizard · 4/5
 *
 * Flying
 * When Pol Jamaar enters, choose a creature type. Draw a card for each creature you control of
 * that type.
 *
 * Distant Melody's resolution-time choose-then-count pipeline on an ETB trigger (Selfless
 * Safewright's shape), counting creatures rather than permanents. Pol Jamaar counts itself when
 * one of its own types (Human, Illusion, Wizard) is chosen.
 */
val PolJamaarIllusionist = card("Pol Jamaar, Illusionist") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Illusion Wizard"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "When Pol Jamaar enters, choose a creature type. Draw a card for each creature you control of that type."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val chosenType = chooseOption(OptionType.CREATURE_TYPE)
            run(
                Effects.DrawCards(
                    count = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Creature.withSubtypeFromVariable(chosenType)
                    ).count()
                )
            )
        }
        description = "When Pol Jamaar enters, choose a creature type. Draw a card for each creature you control of that type."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "38"
        artist = "Kuregure"
        flavorText = "\"'Real' is a more flexible concept than you might think.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63e806ee-3c32-48bf-97a1-160a74765e76.jpg?1783908859"
        ruling(
            "2024-11-08",
            "The choice of creature type is made as Pol Jamaar's last ability resolves. Players can't take any " +
                "actions between the time the choice is made and the time you draw cards."
        )
    }
}
