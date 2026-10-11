package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dollmaker's Shop // Porcelain Gallery (DSK 4) — split-layout Room (CR 709.5).
 *
 * Dollmaker's Shop {1}{W} — Enchantment — Room
 *   Whenever one or more non-Toy creatures you control attack a player, create a 1/1 white Toy
 *   artifact creature token.
 *
 * Porcelain Gallery {4}{W}{W} — Enchantment — Room
 *   Creatures you control have base power and toughness each equal to the number of creatures you
 *   control.
 *
 * Cast each half separately; the cast face enters unlocked, the other locked. Pay the locked
 * face's printed mana cost as a sorcery-speed special action to unlock it (CR 709.5e). Each face's
 * ability only functions while that door is unlocked — the engine gates a Room face's
 * triggered/static abilities on its door state, so the Dollmaker's Shop attack trigger and the
 * Porcelain Gallery lord both go live only once their door is open (cf. Painter's Studio //
 * Defaced Gallery, Restricted Office // Lecture Hall).
 *
 * Porcelain Gallery is a Layer 7b base-P/T set ([SetBasePowerToughnessDynamicStatic]) over every
 * creature you control, with the same single count — creatures you control — feeding both power and
 * toughness (the count is recomputed continuously, so it grows as you add creatures and includes
 * the affected creatures themselves and any tokens). Cf. Titania's Song for the group base-P/T set.
 */
val DollmakersShopPorcelainGallery = card("Dollmaker's Shop // Porcelain Gallery") {
    layout = CardLayout.SPLIT
    colorIdentity = "W"

    face("Dollmaker's Shop") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment — Room"
        oracleText = "Whenever one or more non-Toy creatures you control attack a player, create a " +
            "1/1 white Toy artifact creature token."

        triggeredAbility {
            trigger = Triggers.you.attacksAPlayer(GameObjectFilter.Creature.youControl().notSubtype(Subtype("Toy")))
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Toy"),
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/5/e/5e6d479d-c30d-4048-b250-5233358a174c.jpg?1726236536",
            )
            description = "Whenever one or more non-Toy creatures you control attack a player, " +
                "create a 1/1 white Toy artifact creature token."
        }
    }

    face("Porcelain Gallery") {
        manaCost = "{4}{W}{W}"
        typeLine = "Enchantment — Room"
        oracleText = "Creatures you control have base power and toughness each equal to the number " +
            "of creatures you control."

        staticAbility {
            ability = SetBasePowerToughnessDynamicStatic(
                power = DynamicAmounts.creaturesYouControl(),
                toughness = DynamicAmounts.creaturesYouControl(),
                filter = GroupFilter.AllCreaturesYouControl,
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5ee6651-9946-4bae-b21e-6cf28fa77b13.jpg?1726867800"
    }
}
