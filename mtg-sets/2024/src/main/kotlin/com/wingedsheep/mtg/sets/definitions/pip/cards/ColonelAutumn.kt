package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Colonel Autumn — Fallout #98.
 *
 * Exploit is display-only as a keyword; the behaviour is its enters trigger. So the static grant is
 * two halves over the same group — the `EXPLOIT` badge and [exploitTriggeredAbility] — as Elder Arthur
 * Maxson grants training. The payoff watcher relies on the exploit sacrifice look-back: it fires when
 * Colonel Autumn is itself the creature sacrificed, but not when it left before the exploit resolved.
 */
val ColonelAutumn = card("Colonel Autumn") {
    manaCost = "{1}{W}{B}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Lifelink\nExploit (When this creature enters, you may sacrifice a creature.)\nOther legendary creatures you control have exploit.\nWhenever a creature you control exploits a creature, put a +1/+1 counter on each creature you control."
    keywords(Keyword.LIFELINK)
    exploit()
    val otherLegendaryCreatures = GroupFilter(GameObjectFilter.Creature.legendary().youControl(), excludeSelf = true)
    staticAbility { ability = GrantKeyword(Keyword.EXPLOIT, otherLegendaryCreatures) }
    staticAbility { ability = GrantTriggeredAbility(exploitTriggeredAbility(), otherLegendaryCreatures) }
    triggeredAbility {
        trigger = Triggers.you.exploits()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Daniel Romanovsky"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca0c3e5a-64f3-40b5-8080-1c53be45aa40.jpg?1783912390"
        ruling("2024-03-08", "A creature with exploit “exploits a creature” when the controller of an exploit ability sacrifices a creature as that ability resolves.")
        ruling("2024-03-08", "You choose whether to sacrifice a creature and which creature to sacrifice as an exploit ability resolves.")
        ruling("2024-03-08", "You can sacrifice the creature with exploit if it’s still on the battlefield. If it has an ability that triggers when it exploits a creature (or, in this case, when any creature you control exploits a creature), that ability will trigger.")
        ruling("2024-03-08", "If Colonel Autumn isn’t on the battlefield as an exploit ability of a creature you control resolves, Colonel Autumn’s last ability won’t trigger. You won’t get +1/+1 counters, even if you sacrifice a creature.")
        ruling("2024-03-08", "You can’t sacrifice more than one creature to any one exploit ability.")
    }
}
