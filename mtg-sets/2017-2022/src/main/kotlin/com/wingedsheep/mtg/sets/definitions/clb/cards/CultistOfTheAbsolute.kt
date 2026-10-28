package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cultist of the Absolute — Commander Legends: Battle for Baldur's Gate #123 (canonical / earliest real printing, 2022)
 * {B} · Legendary Enchantment — Background
 *
 * Commander creatures you own get +3/+3 and have flying, deathtouch, "Ward—Pay 3 life," and "At the
 * beginning of your upkeep, sacrifice a creature."
 *
 * Every piece rides the same group — `Creature.commander().ownedByYou()`, Folk Hero's scope — so a
 * commander you own keeps the bonus even under an opponent's control, and with two commander
 * creatures each gets all of it (ruling). The ward is a battlefield-scope [GrantWard] with a
 * [WardCost.Life] cost; the upkeep sacrifice is a [GrantTriggeredAbility], so "your upkeep" and the
 * sacrificing player are the commander's controller. The commander itself is a legal sacrifice.
 */
val CultistOfTheAbsolute = card("Cultist of the Absolute") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment — Background"
    oracleText = "Commander creatures you own get +3/+3 and have flying, deathtouch, \"Ward—Pay 3 life,\" " +
        "and \"At the beginning of your upkeep, sacrifice a creature.\""

    val commanders = GroupFilter(GameObjectFilter.Creature.commander().ownedByYou())

    staticAbility { ability = ModifyStats(powerBonus = 3, toughnessBonus = 3, filter = commanders) }
    staticAbility { ability = GrantKeyword(Keyword.FLYING, commanders) }
    staticAbility { ability = GrantKeyword(Keyword.DEATHTOUCH, commanders) }
    staticAbility { ability = GrantWard(WardCost.Life(3), commanders) }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.Controller),
                descriptionOverride = "At the beginning of your upkeep, sacrifice a creature.",
            ),
            filter = commanders,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Artur Treffner"
        flavorText = "You embrace power at any cost, and if it consumes you, so be it."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0c7b4af-d521-4f4f-8910-7e05f2f60eb6.jpg?1783922764"
        ruling("2022-06-10", "If a card refers to a commander creature you own, a Background won't usually be counted or included for that effect. If another spell or ability causes your Background to become a creature, however, it will be included.")
        ruling("2022-06-10", "If you control a Background that grants an ability to commander creatures you own, and you own more than one commander creature, each of them will have that ability.")
    }
}
