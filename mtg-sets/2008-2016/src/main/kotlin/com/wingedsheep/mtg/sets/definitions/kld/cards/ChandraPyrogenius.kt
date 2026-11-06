package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra, Pyrogenius — Kaladesh #265 (Planeswalker Deck)
 * {4}{R}{R} · Legendary Planeswalker — Chandra · Starting loyalty 5
 *
 * +2: Chandra deals 2 damage to each opponent.
 * −3: Chandra deals 4 damage to target creature.
 * −10: Chandra deals 6 damage to target player or planeswalker and each creature that player or
 *      that planeswalker's controller controls.
 *
 * The −10 is Chandra Nalaar's ultimate shape: only the recipient is targeted, so hexproof creatures
 * it covers are still dealt damage (ruling), and an animated target planeswalker is dealt damage once.
 */
val ChandraPyrogenius = card("Chandra, Pyrogenius") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 5
    oracleText = "+2: Chandra deals 2 damage to each opponent.\n" +
        "−3: Chandra deals 4 damage to target creature.\n" +
        "−10: Chandra deals 6 damage to target player or planeswalker and each creature that player " +
        "or that planeswalker's controller controls."

    loyaltyAbility(+2) {
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature)
    }

    loyaltyAbility(-10) {
        val recipient = target(Targets.PlayerOrPlaneswalker)
        val creatures = GameObjectFilter.Creature.targetPlayerControls(recipient) or
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
        effect = Effects.DealDamage(6, recipient) then
            Patterns.Group.dealDamageToAll(6, GroupFilter(creatures, excludeTarget = true))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "265"
        inBooster = false
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ab8435b-5fe8-4074-8809-46aa6e5504c8.jpg?1783937136"

        ruling("2016-09-20", "The last ability of Chandra, Pyrogenius doesn't target the creatures. A creature with hexproof the target player (or controller of the target planeswalker) controls will be dealt damage this way.")
        ruling("2016-09-20", "In a Two-Headed Giant game, Chandra's first ability causes 4 damage total to be dealt to the opposing team.")
    }
}
