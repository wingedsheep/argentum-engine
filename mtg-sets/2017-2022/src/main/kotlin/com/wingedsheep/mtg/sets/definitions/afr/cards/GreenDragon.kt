package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Green Dragon
 * {4}{G}{G}
 * Creature — Dragon
 * 4/4
 * Flying
 * Poison Breath — When this creature enters, until end of turn, whenever a creature an opponent
 * controls is dealt damage, destroy it.
 *
 * The enters trigger installs a filter-scoped event delayed trigger (`fireOnce = false`,
 * `expiry = EndOfTurn`) on the recipient form of `isDealtDamage()`; the damaged creature is the
 * triggering entity. It lives on its own, so it keeps working after the Dragon leaves (ruling).
 */
val GreenDragon = card("Green Dragon") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nPoison Breath — When this creature enters, until end of turn, whenever a creature " +
        "an opponent controls is dealt damage, destroy it."
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).isDealtDamage(),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45804134-cc04-4587-9674-2c884964083d.jpg?1783926462"
        ruling(
            "2021-07-23",
            "Green Dragon's Poison Breath ability continues to function until end of turn even if Green Dragon is " +
                "destroyed or otherwise leaves the battlefield. It takes some time for toxic gas to dissipate."
        )
        ruling(
            "2021-07-23",
            "Green Dragon's Poison Breath ability is different than deathtouch. Notably, creatures still need to " +
                "deal damage to a blocking creature equal to its toughness before dealing damage to another blocking " +
                "creature or trampling over to a player or planeswalker."
        )
    }
}
