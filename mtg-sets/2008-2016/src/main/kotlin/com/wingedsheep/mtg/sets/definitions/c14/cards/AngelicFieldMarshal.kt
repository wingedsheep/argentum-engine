package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Angelic Field Marshal — Commander 2014 #2
 * {2}{W}{W} · Creature — Angel · 3/3
 * Flying
 * Lieutenant — As long as you control your commander, this creature gets +2/+2 and creatures you
 * control have vigilance.
 *
 * "Your commander" is a commander you own that is on the battlefield under your control — the same
 * `Permanent.commander().ownedByYou()` gate Siege-Gang Lieutenant and Loyal Apprentice use, here
 * wrapped around a static ability rather than a trigger. It is read from the current controller of
 * this creature, so a stolen Field Marshal checks the thief's commander (ruling).
 */
val AngelicFieldMarshal = card("Angelic Field Marshal") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Lieutenant — As long as you control your commander, this creature gets +2/+2 and creatures " +
        "you control have vigilance."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source()),
                    GrantKeyword(Keyword.VIGILANCE, GroupFilter.AllCreaturesYouControl),
                )
            ),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.commander().ownedByYou()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Scott Murphy"
        flavorText = "Her presence on the battlefield can transform an unruly mob into a disciplined army."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e8185b9-f559-47e8-ae90-d62396b4aa6b.jpg?1783938874"
        ruling("2014-11-07", "Lieutenant abilities apply only if your commander is on the battlefield and under your control.")
        ruling("2014-11-07", "Lieutenant abilities refer only to whether you control your commander, not any other player’s commander.")
        ruling("2014-11-07", "If you gain control of a creature with a lieutenant ability owned by another player, that ability will check to see if you control your commander and will apply if you do. It won’t check whether its owner controls their commander.")
        ruling("2014-11-07", "If you lose control of your commander, lieutenant abilities of creatures you control will immediately stop applying. If this causes a creature’s toughness to become less than or equal to the amount of damage marked on it, the creature will be destroyed.")
    }
}
