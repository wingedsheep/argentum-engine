package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Somberwald Vigilante
 * {R}
 * Creature — Human Warrior
 * 1/1
 *
 * Whenever this creature becomes blocked by a creature, this creature deals 1 damage to that creature.
 *
 * `becomesBlocked(by = ...)` fires once per blocking creature, with that blocker as the
 * triggering entity — "that creature" is not a target.
 */
val SomberwaldVigilante = card("Somberwald Vigilante") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature becomes blocked by a creature, this creature deals 1 damage to that creature."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = Filters.Creature)
        effect = Effects.DealDamage(1, EffectTarget.TriggeringEntity, damageSource = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "John Stanko"
        flavorText = "He has nothing left but his resentment."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e69eb29-a7e4-4826-9535-9a073136c688.jpg?1783940677"
        ruling("2012-05-01", "Somberwald Vigilante's ability will trigger for each creature it becomes blocked by.")
        ruling("2012-05-01", "The ability resolves before combat damage is dealt, so it could cause blocking creatures to be destroyed and thus not deal combat damage.")
    }
}
