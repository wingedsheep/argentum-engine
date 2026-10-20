package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ivory Guardians
 * {4}{W}{W}
 * Creature — Giant Cleric
 * 3/3
 * Protection from red
 * Creatures named Ivory Guardians get +1/+1 as long as an opponent controls a nontoken red permanent.
 *
 * The Beasts of Bogardan shape widened to a name-based lord: every creature named Ivory Guardians,
 * whoever controls it, gets the bonus — "an opponent" is read relative to this Guardians'
 * controller. Each copy is its own static ability, so the bonuses stack (two Guardians make each
 * other 5/5), while one ability applies at most once no matter how many red permanents exist.
 */
val IvoryGuardians = card("Ivory Guardians") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Cleric"
    power = 3
    toughness = 3
    oracleText = "Protection from red\n" +
        "Creatures named Ivory Guardians get +1/+1 as long as an opponent controls a nontoken red permanent."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 1,
                filter = GroupFilter(GameObjectFilter.Creature.named("Ivory Guardians")),
            ),
            condition = Conditions.OpponentControls(
                GameObjectFilter.Permanent.withColor(Color.RED).nontoken()
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Melissa A. Benson"
        flavorText = "The elite guard of the Mesa High Priests, the Ivory Guardians, were created to protect " +
            "the innocent and faithful. Some say their actions are above the law."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bf9cccd-fe97-4632-a90a-9eeb0d41135e.jpg?1783948084"
        ruling(
            "2009-10-01",
            "The ability of an Ivory Guardians will affect itself as well as all other creatures named " +
                "Ivory Guardians (not just the ones you control)."
        )
        ruling(
            "2009-10-01",
            "Each ability applies a maximum of once, even if an opponent controls multiple nontoken red " +
                "permanents or multiple opponents each control a nontoken red permanent."
        )
        ruling(
            "2009-10-01",
            "The abilities from multiple Ivory Guardians are cumulative. For example, if you control two " +
                "different Ivory Guardians and an opponent controls a nontoken red permanent, each of your " +
                "Ivory Guardians will be 5/5."
        )
    }
}
