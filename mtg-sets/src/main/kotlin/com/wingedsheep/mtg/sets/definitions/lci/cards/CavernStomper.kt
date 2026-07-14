package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavern Stomper
 * {4}{G}{G}
 * Creature — Dinosaur
 * 7/7
 *
 * When this creature enters, scry 2.
 * {3}{G}: This creature can't be blocked by creatures with power 2 or less this turn.
 *
 * The evasion ability grants a one-turn [CantBeBlockedBy] restriction to the source itself
 * ([EffectTarget.Self], [com.wingedsheep.sdk.scripting.Duration.EndOfTurn] default), keyed on
 * blocker power via [GameObjectFilter.Creature.powerAtMost]. Any-speed activation, repeatable.
 */
val CavernStomper = card("Cavern Stomper") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    oracleText = "When this creature enters, scry 2.\n" +
        "{3}{G}: This creature can't be blocked by creatures with power 2 or less this turn."
    power = 7
    toughness = 7

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(2)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.GrantStaticAbility(
            CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "David Szabo"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/decdfd6b-55ab-47fd-9f98-1845261f1caf.jpg?1782694468"
    }
}
