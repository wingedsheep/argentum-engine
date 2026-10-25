package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Heroes' Podium
 * {5}
 * Legendary Artifact
 * Each legendary creature you control gets +1/+1 for each other legendary creature you control.
 * {X}, {T}: Look at the top X cards of your library. You may reveal a legendary creature card from
 * among them and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * - Every creature the bonus applies to is itself a legendary creature you control, so "each other
 *   legendary creature you control" is the count of legendary creatures you control minus one, for
 *   each recipient alike. Projected types, so a creature made legendary (or animated) counts.
 * - The activated ability is the look-at-top-X / reveal-a-match-to-hand / rest-on-the-bottom-randomly
 *   shape ([Patterns.Library.lookAtTopRevealMatchingToHand]).
 */
private val legendaryCreatures = GameObjectFilter.Creature.legendary()

val HeroesPodium = card("Heroes' Podium") {
    manaCost = "{5}"
    typeLine = "Legendary Artifact"
    oracleText = "Each legendary creature you control gets +1/+1 for each other legendary creature you control.\n" +
        "{X}, {T}: Look at the top X cards of your library. You may reveal a legendary creature card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    staticAbility {
        val otherLegends = DynamicAmounts.battlefield(Player.You, legendaryCreatures).count() - 1
        ability = GrantDynamicStats(
            filter = GroupFilter(legendaryCreatures.youControl()),
            powerBonus = otherLegends,
            toughnessBonus = otherLegends,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmounts.xValue(),
            filter = legendaryCreatures,
            prompt = "You may reveal a legendary creature card from among them and put it into your hand",
        )
        description = "Look at the top X cards of your library. You may reveal a legendary creature card " +
            "from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3cb14f9-343c-4672-b4ee-db7f1d1a98ff.jpg?1783939521"
        ruling("2014-02-01", "The bonus given by Heroes' Podium counts only legendary creatures. It won't count itself unless some other effect causes it to be a creature in addition to being a land (in which case it will also get the bonus).")
        ruling("2014-02-01", "Heroes' Podium will give a bonus to each legendary creature you control, even if gaining control of one causes the \"legend rule\" to apply. For example, if you control Brimaz, King of Oreskos (a 3/4 legendary creature), and gain control of another one, they'll each be 4/5 when you put one into its owner's graveyard. Then the remaining one will return to being 3/4.")
    }
}
