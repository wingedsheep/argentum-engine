package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/** Savage Alliance — Eldritch Moon #140. */
val SavageAlliance = card("Savage Alliance") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Escalate {1} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or more —\n" +
        "• Creatures target player controls gain trample until end of turn.\n" +
        "• Savage Alliance deals 2 damage to target creature.\n" +
        "• Savage Alliance deals 1 damage to each creature target opponent controls."

    spell {
        modal(chooseCount = 3, minChooseCount = 1, additionalManaCostPerExtraMode = "{1}") {
            mode("Creatures target player controls gain trample until end of turn.") {
                val player = target("trample player", TargetPlayer())
                effect = Patterns.Group.grantKeywordToAll(
                    keyword = Keyword.TRAMPLE,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                )
            }
            mode("Savage Alliance deals 2 damage to target creature.") {
                val creature = target("damage creature", TargetCreature())
                effect = Effects.DealDamage(2, creature)
            }
            mode("Savage Alliance deals 1 damage to each creature target opponent controls.") {
                val opponent = target("damage opponent", TargetOpponent())
                effect = Patterns.Group.dealDamageToAll(
                    amount = 1,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5255da8-8511-48a7-98e5-ba43ca6e8681.jpg?1783937456"
        ruling("2016-07-13", "If the second and third modes affect the same creature, it is dealt 2 damage and 1 damage as separate events.")
        ruling("2016-07-13", "If one target becomes illegal, the other targets are still affected. If all targets become illegal, the spell doesn't resolve.")
    }
}
