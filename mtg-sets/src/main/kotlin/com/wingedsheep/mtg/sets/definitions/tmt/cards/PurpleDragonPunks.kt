package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Purple Dragon Punks
 * {1}{R}
 * Creature — Human Rogue
 * 2/2
 *
 * {T}: Add {R}. Spend this mana only to cast an artifact spell or to activate an ability.
 */
val PurpleDragonPunks = card("Purple Dragon Punks") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    oracleText = "{T}: Add {R}. Spend this mana only to cast an artifact spell or to activate an ability."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(
            Color.RED,
            1,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                        CardType.ARTIFACT,
                        allowSpells = true,
                        allowAbilities = false
                    ),
                    ManaRestriction.AbilityActivationOnly
                )
            )
        )
        description = "{T}: Add {R}. Spend this mana only to cast an artifact spell or to activate an ability."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Rose Benjamin"
        flavorText = "\"These guys are young, but no rookies. They've fought and beat everything on two legs in this area . . . except us.\"\n—Leonardo"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbe1d7e5-68e2-4458-aa3f-5e97fa8e7cae.jpg?1771586948"
    }
}
