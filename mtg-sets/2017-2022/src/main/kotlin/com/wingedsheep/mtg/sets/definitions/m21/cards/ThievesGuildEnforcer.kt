package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thieves' Guild Enforcer
 * {B}
 * Creature — Human Rogue
 * 1/1
 * Flash
 * Whenever this creature or another Rogue you control enters, each opponent mills two cards.
 * As long as an opponent has eight or more cards in their graveyard, this creature gets +2/+1 and
 * has deathtouch.
 *
 * "This creature or another Rogue you control" is the house `Triggers.a(Rogue you control)` reading
 * (the Enforcer is itself a Rogue). "An opponent has eight or more" is Blackbloom Rogue's
 * per-opponent test — the greatest graveyard among opponents, so the bonus applies once (ruling).
 */
val ThievesGuildEnforcer = card("Thieves' Guild Enforcer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "Flash\n" +
        "Whenever this creature or another Rogue you control enters, each opponent mills two cards.\n" +
        "As long as an opponent has eight or more cards in their graveyard, this creature gets +2/+1 " +
        "and has deathtouch."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Rogue").youControl()).enters()
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.greatestAmongPlayers(
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                Player.EachOpponent
            ),
            ComparisonOperator.GTE,
            8
        )
        ability = CompositeStaticAbility(
            listOf(
                ModifyStats(powerBonus = 2, toughnessBonus = 1, filter = GroupFilter.source()),
                GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.source()),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df99f770-2c39-4025-a8a2-a5890f61eb53.jpg?1783930698"
        ruling(
            "2020-06-23",
            "Thieves' Guild Enforcer gets +2/+1 just once, no matter how many opponents have eight or " +
                "more cards in their graveyard and no matter how many cards they have past the eighth."
        )
        ruling(
            "2020-06-23",
            "If Thieves' Guild Enforcer gains deathtouch only after dealing damage (most likely because " +
                "another creature was dealt lethal damage at the same time that Thieves' Guild Enforcer " +
                "dealt damage), the damage it dealt won't be from a source with deathtouch."
        )
    }
}
