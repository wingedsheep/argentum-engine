package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Akki Ember-Keeper — Kamigawa: Neon Dynasty #130 (canonical printing)
 * {1}{R} · Enchantment Creature — Goblin Warrior · 2/1
 *
 * Whenever a nontoken modified creature you control dies, create a 1/1 colorless Spirit creature
 * token.
 *
 * "Modified" (CR 700.9) is [StatePredicate.IsModified]; on a dies trigger the trigger matcher
 * answers it from the departing creature's last-known information (counters, attached Equipment,
 * Auras its controller controls), so a creature that was modified as it died triggers this. The
 * filter has no "another", so Akki Ember-Keeper itself dying while modified triggers it too.
 */
private val nontokenModifiedCreatureYouControl =
    GameObjectFilter.Creature.youControl().nontoken().withStatePredicate(StatePredicate.IsModified)

val AkkiEmberKeeper = card("Akki Ember-Keeper") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Goblin Warrior"
    power = 2
    toughness = 1
    oracleText = "Whenever a nontoken modified creature you control dies, create a 1/1 colorless Spirit " +
        "creature token. (Equipment, Auras you control, and counters are modifications.)"

    triggeredAbility {
        trigger = Triggers.a(nontokenModifiedCreatureYouControl).dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Spirit"),
            imageUri = "https://cards.scryfall.io/normal/front/c/a/ca20548f-6324-4858-adbe-87303ff1ca52.jpg?1783923715"
        )
        description = "Whenever a nontoken modified creature you control dies, create a 1/1 colorless " +
            "Spirit creature token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "April Prime"
        flavorText = "\"Follow my lantern, little ones.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d83e837-fcd1-4833-9117-e6f1bb53caad.jpg?1783923873"
    }
}
