package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bonfire of the Damned
 * {X}{X}{R}
 * Sorcery
 *
 * Bonfire of the Damned deals X damage to target player or planeswalker and each creature that
 * player or that planeswalker's controller controls.
 * Miracle {X}{R}
 *
 * Mirrors Chandra Nalaar's −8: only the player or planeswalker is targeted; the creature group is
 * "creatures the targeted player controls" or "creatures the targeted planeswalker's controller
 * controls", excluding the target itself so an animated planeswalker is dealt X only once. X is read
 * at resolution from whichever cost was paid ({X}{X} printed, or {X} via miracle), as Entreat the
 * Angels does.
 */
val BonfireOfTheDamned = card("Bonfire of the Damned") {
    manaCost = "{X}{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Bonfire of the Damned deals X damage to target player or planeswalker and each creature " +
        "that player or that planeswalker's controller controls.\n" +
        "Miracle {X}{R} (You may cast this card for its miracle cost when you draw it if it's the first " +
        "card you drew this turn.)"

    spell {
        val recipient = target(Targets.PlayerOrPlaneswalker)
        val creatures = GameObjectFilter.Creature.targetPlayerControls(recipient) or
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), recipient) then
            Patterns.Group.dealDamageToAll(
                DynamicAmounts.xValue(),
                GroupFilter(creatures, excludeTarget = true),
            )
    }

    keywordAbility(KeywordAbility.miracle("{X}{R}"))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "129"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e60610fe-891d-46de-b556-d03b637dccec.jpg?1783940689"

        ruling("2017-03-14", "Bonfire of the Damned targets only the player. Creatures with shroud or hexproof that player controls will be dealt damage by Bonfire of the Damned if it resolves.")
        ruling("2017-03-14", "A mana cost of {X}{X} means that you pay twice X. If you want X to be 3, you pay {6}{R} to cast Bonfire of the Damned.")
        ruling("2017-03-14", "Bonfire of the Damned’s mana value is based on its mana cost of {X}{X}{R}, even if you’re casting it for its miracle cost. For example, if you cast Bonfire of the Damned for its miracle cost and choose 4 for X, its mana value is 9.")
        ruling("2017-03-14", "If the player is an illegal target when Bonfire of the Damned tries to resolve, the spell won’t resolve and none of its effects will happen. No damage will be dealt to any permanent or player.")
        ruling("2017-03-14", "Preventing some or all of the damage dealt to the player won’t affect the amount of damage dealt to each creature that player controls. The same is true for any effect that increases or decreases the amount of damage dealt to the player.")
    }
}
