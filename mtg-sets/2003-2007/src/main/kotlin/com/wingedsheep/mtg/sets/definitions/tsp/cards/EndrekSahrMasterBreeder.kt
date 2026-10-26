package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Endrek Sahr, Master Breeder
 * {4}{B}
 * Legendary Creature — Human Wizard
 * 2/2
 *
 * Whenever you cast a creature spell, create X 1/1 black Thrull creature tokens, where X is that
 * spell's mana value.
 * When you control seven or more Thrulls, sacrifice Endrek Sahr.
 *
 * "That spell's mana value" reads [DynamicAmounts.triggeringSpellManaValue], captured on the cast
 * event, so it stays right if the spell is countered before the trigger resolves. The second
 * ability is a state trigger (CR 603.8): once on the stack it sacrifices Endrek Sahr on resolution
 * however many Thrulls remain, per the 2006-09-25 ruling.
 */
val EndrekSahrMasterBreeder = card("Endrek Sahr, Master Breeder") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Wizard"
    oracleText = "Whenever you cast a creature spell, create X 1/1 black Thrull creature tokens, where X is that spell's mana value.\n" +
        "When you control seven or more Thrulls, sacrifice Endrek Sahr."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.CreateToken(
            count = DynamicAmounts.triggeringSpellManaValue(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Thrull"),
            imageUri = "https://cards.scryfall.io/normal/front/d/c/dc5604c5-4fff-4bb6-bf6d-69027e4eeb21.jpg?1783915255"
        )
        description = "Whenever you cast a creature spell, create X 1/1 black Thrull creature tokens, where X is that spell's mana value."
    }

    stateTriggeredAbility {
        condition = Conditions.YouControlAtLeast(7, GameObjectFilter.Permanent.withSubtype("Thrull"))
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control seven or more Thrulls, sacrifice Endrek Sahr."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dac6faba-59f2-436f-addb-c53db2b0cc17.jpg?1783943233"
        ruling("2006-09-25", "The second ability is a state trigger. Once it triggers, it won't trigger again as long as the ability is on the stack. If the ability is countered and the trigger condition is still true, it will immediately trigger again.")
        ruling("2006-09-25", "When the second ability resolves, Endrek Sahr will be sacrificed regardless of how many Thrulls you control at that time, even if it's less than seven.")
        ruling("2006-09-25", "Face-down spells are creature spells, but they have mana value 0.")
    }
}
