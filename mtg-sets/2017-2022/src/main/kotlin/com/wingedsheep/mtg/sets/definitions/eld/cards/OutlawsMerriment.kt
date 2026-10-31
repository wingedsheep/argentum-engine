package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect

private val RedAndWhite = setOf(Color.RED, Color.WHITE)

/**
 * Outlaws' Merriment — Throne of Eldraine #198
 * {1}{R}{W}{W} · Enchantment
 *
 * At the beginning of your upkeep, choose one at random. Create a red and white creature token
 * with those characteristics.
 * • 3/1 Human Warrior with trample and haste.
 * • 2/1 Human Cleric with lifelink and haste.
 * • 1/2 Human Rogue with haste and "When this token enters, it deals 1 damage to any target."
 *
 * `ModalEffect.chooseOneAtRandom` draws the mode as the upkeep trigger goes on the stack, so
 * players can respond knowing which token is coming (ruling). The Rogue carries its enters
 * trigger as a token-level ability; its target is chosen when that trigger goes on the stack.
 */
val OutlawsMerriment = card("Outlaws' Merriment") {
    manaCost = "{1}{R}{W}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, choose one at random. Create a red and white creature " +
        "token with those characteristics.\n" +
        "• 3/1 Human Warrior with trample and haste.\n" +
        "• 2/1 Human Cleric with lifelink and haste.\n" +
        "• 1/2 Human Rogue with haste and \"When this token enters, it deals 1 damage to any target.\""

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = ModalEffect.chooseOneAtRandom(
            mode("Create a 3/1 red and white Human Warrior with trample and haste") {
                effect = Effects.CreateToken(
                    power = 3,
                    toughness = 1,
                    colors = RedAndWhite,
                    creatureTypes = setOf("Human", "Warrior"),
                    keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
                    imageUri = "https://cards.scryfall.io/normal/front/c/9/c994ea90-71f4-403f-9418-2b72cc2de14d.jpg?1783932479",
                )
            },
            mode("Create a 2/1 red and white Human Cleric with lifelink and haste") {
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 1,
                    colors = RedAndWhite,
                    creatureTypes = setOf("Human", "Cleric"),
                    keywords = setOf(Keyword.LIFELINK, Keyword.HASTE),
                    imageUri = "https://cards.scryfall.io/normal/front/d/b/db951f76-b785-453e-91b9-b3b8a5c1cfd4.jpg?1783932481",
                )
            },
            mode("Create a 1/2 red and white Human Rogue with haste and \"When this token enters, it deals 1 damage to any target\"") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 2,
                    colors = RedAndWhite,
                    creatureTypes = setOf("Human", "Rogue"),
                    keywords = setOf(Keyword.HASTE),
                    triggeredAbilities = listOf(
                        grantedTriggeredAbility {
                            trigger = Triggers.self.enters()
                            val any = target(Targets.Any)
                            effect = Effects.DealDamage(1, any)
                            description = "When this token enters, it deals 1 damage to any target."
                        }
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/c/d/cd3ca6d5-4b2c-46d4-95f3-f0f2fa47f447.jpg?1783932481",
                )
            },
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "198"
        artist = "Suzanne Helmigh"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d7585ab-a364-471c-8ef1-318e459b4020.jpg?1783932597"
        ruling(
            "2019-10-04",
            "As you put the triggered ability of Outlaws' Merriment on the stack, you choose a mode at " +
                "random. Players can respond to the ability knowing which token will be created."
        )
    }
}
