package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Call of the Full Moon
 * {1}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +3/+2 and has trample.
 * At the beginning of each upkeep, if a player cast two or more spells last turn, sacrifice this Aura.
 *
 * The upkeep clause is the Innistrad werewolf "back face" check (Bane of Hanweir): an intervening
 * "if" over `DynamicAmounts.mostSpellsCastLastTurnByAPlayer()` — the greatest single player's count
 * of spells cast during the previous turn, every player's spells included (an opponent's instants on
 * your turn count for that opponent). One spell from each of two players doesn't trigger it, matching
 * the ruling. The count covers the whole previous turn, so casting this Aura plus another spell in
 * one turn sacrifices it at the next upkeep.
 */
val CallOfTheFullMoon = card("Call of the Full Moon") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/+2 and has trample. (It can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/369d837a-a19b-457c-8146-cc1766a63ec1.jpg?1783938333"
        ruling(
            "2015-06-22",
            "Call of the Full Moon's last ability will look at the entire previous turn, even if Call of the Moon wasn't on the battlefield for some or all of that turn. For example, if you cast Call of the Full Moon and another spell on the same turn, you'll have to sacrifice Call of the Full Moon at the beginning of the upkeep of the following turn."
        )
        ruling(
            "2015-06-22",
            "A single player must have cast two or more spells during the previous turn for Call of the Full Moon's last ability to trigger. If multiple players each cast just one spell during that turn, the ability won't trigger."
        )
    }
}
