package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Poetic Ingenuity
 * {2}{R}
 * Enchantment
 *
 * Whenever one or more Dinosaurs you control attack, create that many Treasure tokens.
 * Whenever you cast an artifact spell, create a 3/1 red Dinosaur creature token. This
 * ability triggers only once each turn.
 *
 * Modeling notes:
 *  - The attack trigger is a once-per-combat group trigger ([Triggers.YouAttackWithFilter])
 *    keyed on Dinosaurs you control — it fires once no matter how many Dinosaurs attack, not
 *    once per Dinosaur. "That many Treasure tokens" reads the number of attacking Dinosaurs
 *    at resolution via [DynamicAmount.AggregateBattlefield] over attacking Dinosaurs
 *    (mirrors Choco, Seeker of Paradise's "look at that many cards" attacker-count idiom).
 *    Known deviation: per the ruling, "that many" is fixed to the number of Dinosaurs that
 *    attacked, so one removed from combat in response should still count; the resolution-time
 *    battlefield read undercounts in that corner. Fixing it needs a trigger-snapshot count
 *    primitive (add-feature) that no card in the engine has yet.
 *  - The artifact-cast trigger uses the first-class `oncePerTurn = true` cap ("This ability
 *    triggers only once each turn") — after it fires once in a turn, further artifact spells
 *    that turn do not re-trigger it.
 *  - The 3/1 red Dinosaur token has no imageUri because no separate token card exists in the
 *    LCI dump for this stats/type combination (same as Bonehoard Dracosaur's token).
 */
val PoeticIngenuity = card("Poetic Ingenuity") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more Dinosaurs you control attack, create that many Treasure tokens.\n" +
        "Whenever you cast an artifact spell, create a 3/1 red Dinosaur creature token. " +
        "This ability triggers only once each turn."

    // Whenever one or more Dinosaurs you control attack, create that many Treasure tokens.
    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Creature.withSubtype(Subtype.DINOSAUR))
        effect = Effects.CreateTreasure(
            count = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.DINOSAUR).attacking()
            ),
        )
    }

    // Whenever you cast an artifact spell, create a 3/1 red Dinosaur creature token.
    // This ability triggers only once each turn.
    triggeredAbility {
        trigger = Triggers.youCastSpell(spellFilter = GameObjectFilter.Artifact)
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 3,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dinosaur"),
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee0702f9-769b-40c0-96a7-508dc8f2652c.jpg?1783913606",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Kieran Yanner"
        flavorText = "\"You inspire me.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c035250e-6f6e-4d0f-b4fd-2a53d6069aa7.jpg?1782694481"
    }
}
