package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Palani's Hatcher
 * {3}{R}{G}
 * Creature — Dinosaur
 * 5/3
 *
 * Other Dinosaurs you control have haste.
 * When this creature enters, create two 0/1 green Dinosaur Egg creature tokens.
 * At the beginning of combat on your turn, if you control one or more Eggs,
 * sacrifice an Egg, then create a 3/3 green Dinosaur creature token.
 */
val PalanisHatcher = card("Palani's Hatcher") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 3
    oracleText = "Other Dinosaurs you control have haste.\n" +
        "When this creature enters, create two 0/1 green Dinosaur Egg creature tokens.\n" +
        "At the beginning of combat on your turn, if you control one or more Eggs, sacrifice an Egg, then create a 3/3 green Dinosaur creature token."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype("Dinosaur").youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Dinosaur", "Egg"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/a/7/a7e24a8f-3663-47c6-94b1-4525ae9da3b5.jpg?1699017427"
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.YouControl(GameObjectFilter.Creature.withSubtype("Egg"))
        effect = Effects.Composite(listOf(
            ForceSacrificeEffect(
                filter = GameObjectFilter.Creature.withSubtype("Egg"),
                count = 1,
                target = EffectTarget.PlayerRef(Player.You)
            ),
            Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Dinosaur"),
                count = 1,
                imageUri = "https://cards.scryfall.io/normal/front/2/b/2bbb7151-cf71-49bc-8d99-b0230d5465e5.jpg?1699017364"
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86ff73c7-428c-469c-b564-6aa9f4eeca14.jpg?1699044564"
    }
}
