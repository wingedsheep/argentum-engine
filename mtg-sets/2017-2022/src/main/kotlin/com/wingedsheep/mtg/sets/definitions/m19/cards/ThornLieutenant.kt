package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thorn Lieutenant — Core Set 2019 #203 (canonical printing)
 * {1}{G} Creature — Elf Warrior 2/3
 * Whenever this creature becomes the target of a spell or ability an opponent controls, create a
 * 1/1 green Elf Warrior creature token.
 * {5}{G}: This creature gets +4/+4 until end of turn.
 */
val ThornLieutenant = card("Thorn Lieutenant") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature becomes the target of a spell or ability an opponent controls, " +
        "create a 1/1 green Elf Warrior creature token.\n" +
        "{5}{G}: This creature gets +4/+4 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(byOpponent = true)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/8/0/808b9201-6fc5-4613-85d6-6e66c6e36a8e.jpg?1783934463"
        )
        description = "Whenever this creature becomes the target of a spell or ability an opponent " +
            "controls, create a 1/1 green Elf Warrior creature token."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}")
        effect = Effects.ModifyStats(4, 4, EffectTarget.Self)
        description = "This creature gets +4/+4 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/0812e942-eb78-48c8-857e-5f0ff1bd777b.jpg?1783934528"
        ruling("2018-07-13", "Thorn Lieutenant's triggered ability resolves before the spell or ability that caused it to trigger. It resolves even if that spell or ability is countered.")
    }
}
