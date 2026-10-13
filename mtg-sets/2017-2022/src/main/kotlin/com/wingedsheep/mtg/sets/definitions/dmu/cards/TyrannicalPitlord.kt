package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tyrannical Pitlord
 * {4}{B}{B}
 * Creature — Demon
 * 6/6
 * Flying, trample
 * As this creature enters, choose another creature you control.
 * The chosen creature gets +3/+3 and has flying.
 * When this creature leaves the battlefield, sacrifice the chosen creature.
 *
 * The bonus is a static over [GroupFilter.chosenCreature], so it is a real continuous effect: it
 * follows the chosen creature to a new controller and switches off for good once that object
 * leaves. The leaves trigger reads the choice as it last existed, and [Effects.SacrificeTarget]
 * only sacrifices it while you still control it.
 */
val TyrannicalPitlord = card("Tyrannical Pitlord") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying, trample\n" +
        "As this creature enters, choose another creature you control.\n" +
        "The chosen creature gets +3/+3 and has flying.\n" +
        "When this creature leaves the battlefield, sacrifice the chosen creature."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD))

    staticAbility {
        ability = ModifyStats(3, 3, GroupFilter.chosenCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.chosenCreature())
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.SacrificeTarget(EffectTarget.ChosenCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/027ef1fc-4a7a-465c-9c25-dc19c771b5f5.jpg?1783921246"

        ruling("2022-09-09", "The ability of Tyrannical Pitlord that allows you to choose a creature isn't a triggered ability and doesn't use the stack. Players can't respond to your choice of which creature it's affecting.")
        ruling("2022-09-09", "If Tyrannical Pitlord enters the battlefield at the same time as another creature, that creature can't be chosen.")
        ruling("2022-09-09", "If the chosen creature leaves the battlefield, you can't choose a new creature.")
        ruling("2022-09-09", "If another player gains control of the chosen creature while Tyrannical Pitlord is still on the battlefield, that creature will continue to have flying and get +3/+3.")
        ruling("2022-09-09", "If Tyrannical Pitlord's leaves-the-battlefield triggered ability resolves while its controller is not the same as the controller of the chosen creature, that creature will not be sacrificed.")
    }
}
