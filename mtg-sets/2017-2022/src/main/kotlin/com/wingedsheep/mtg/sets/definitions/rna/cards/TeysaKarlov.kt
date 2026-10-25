package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalDeathTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Teysa Karlov
 * {2}{W}{B}
 * Legendary Creature — Human Advisor
 * 2/4
 *
 * If a creature dying causes a triggered ability of a permanent you control to trigger, that
 * ability triggers an additional time.
 * Creature tokens you control have vigilance and lifelink.
 */
val TeysaKarlov = card("Teysa Karlov") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 4
    oracleText = "If a creature dying causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time.\n" +
        "Creature tokens you control have vigilance and lifelink."

    staticAbility {
        ability = AdditionalDeathTriggers(permanentsYouControl = GameObjectFilter.Any)
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Token).youControl())
        )
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.LIFELINK,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Token).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Magali Villeneuve"
        flavorText = "While Kaya is guildmaster, Teysa waits patiently at the center of the Orzhov web of power."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcfaa19e-995e-447d-a0a2-46e5d117d5ec.jpg?1783933635"
        ruling("2019-01-25", "Teysa affects a creature's own \"when this creature dies\" triggered abilities as well as other triggered abilities that trigger when that creature dies.")
        ruling("2019-01-25", "Teysa's effect doesn't copy the triggered ability; it just causes the ability to trigger twice. Choices such as modes and targets are made separately for each instance.")
        ruling("2019-01-25", "An ability that triggers when a creature \"leaves the battlefield\" will trigger twice if that creature leaves the battlefield by dying.")
        ruling("2019-01-25", "An ability that triggers on an event that causes a creature to die doesn't trigger twice. For example, an ability that triggers \"whenever you sacrifice a creature\" triggers only once.")
        ruling("2019-01-25", "If a creature dying at the same time as Teysa (including Teysa itself dying) causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time.")
        ruling("2019-01-25", "If you somehow control two Teysas, a creature dying causes abilities to trigger three times, not four.")
    }
}
