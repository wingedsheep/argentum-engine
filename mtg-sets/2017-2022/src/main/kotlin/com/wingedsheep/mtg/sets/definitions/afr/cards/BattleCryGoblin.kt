package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battle Cry Goblin
 * {1}{R}
 * Creature — Goblin
 * 2/2
 * {1}{R}: Goblins you control get +1/+0 and gain haste until end of turn.
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, create a 1/1 red Goblin creature token that's tapped and attacking.
 *
 * "Goblins" is the bare tribal noun, so the group is Goblin permanents rather than Goblin
 * creatures; the pump and haste grant gather that group once via [Patterns.Group.pumpAndGrantToAll].
 */
val BattleCryGoblin = card("Battle Cry Goblin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "{1}{R}: Goblins you control get +1/+0 and gain haste until end of turn.\n" +
        "Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6 " +
        "or greater this combat, create a 1/1 red Goblin creature token that's tapped and attacking."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 0,
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1783926342",
            tapped = true,
            attacking = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "April Prime"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9766a427-2bb3-4028-a502-d1194cdc93aa.jpg?1783926484"
    }
}
